//! 会话录像：终端输出流按阈值切片，异步上传 MinIO（S3 兼容），索引事件发 RabbitMQ。
//!
//! 切片规则：累计 64 KiB 或距上次切片 5 秒，先到先切。
//! 对象路径：`sessions/{session_id}/{seq}.cast`（.cast 为堡垒机录像通用扩展名，内容为原始字节流）。

use anyhow::{anyhow, Result};
use chrono::Utc;
use s3::creds::Credentials;
use s3::{Bucket, Region};
use std::sync::Arc;
use tokio::sync::Mutex;

use crate::audit::{
    emit_recording_index, EventPublisher, RecordingIndexData,
};

/// 切片触发阈值。
const SLICE_BYTES: usize = 64 * 1024;
const SLICE_INTERVAL_SECS: i64 = 5;

/// 会话录像记录器：feed 终端输出字节，内部自动切片上传。
pub struct Recorder {
    session_id: i64,
    bucket: Box<Bucket>,
    publisher: Arc<dyn EventPublisher>,
    /// 当前切片缓冲
    buf: Vec<u8>,
    seq: u32,
    last_slice_at: chrono::DateTime<Utc>,
    /// 上传互斥（切片上传串行，避免同 key 竞争）
    upload_lock: Arc<Mutex<()>>,
}

impl Recorder {
    pub fn new(
        session_id: i64,
        minio_endpoint: &str,
        access_key: &str,
        secret_key: &str,
        bucket_name: &str,
        publisher: Arc<dyn EventPublisher>,
    ) -> Result<Self> {
        let credentials = Credentials::new(
            Some(access_key),
            Some(secret_key),
            None,
            None,
            None,
        )
        .map_err(|err| anyhow!("MinIO 凭据构建失败: {err}"))?;
        let region = Region::Custom {
            region: "us-east-1".into(),
            endpoint: minio_endpoint.to_string(),
        };
        // MinIO 必须用 path-style 寻址（默认 subdomain 风格会把桶名拼进 host 导致解析失败）
        let bucket = Bucket::new(bucket_name, region, credentials)
            .map_err(|err| anyhow!("MinIO bucket 构建失败: {err}"))?
            .with_path_style();
        Ok(Self {
            session_id,
            bucket,
            publisher,
            buf: Vec::with_capacity(SLICE_BYTES),
            seq: 0,
            last_slice_at: Utc::now(),
            upload_lock: Arc::new(Mutex::new(())),
        })
    }

    /// 喂入终端输出字节；达到阈值时自动切片上传（异步，不阻塞会话流）。
    pub fn feed(&mut self, data: &[u8]) {
        self.buf.extend_from_slice(data);
        let due_bytes = self.buf.len() >= SLICE_BYTES;
        let due_time = (Utc::now() - self.last_slice_at).num_seconds() >= SLICE_INTERVAL_SECS;
        if due_bytes || due_time {
            self.flush();
        }
    }

    /// 强制切片（会话结束时调用）。
    pub fn flush(&mut self) {
        if self.buf.is_empty() {
            return;
        }
        let seq = self.seq;
        self.seq += 1;
        self.last_slice_at = Utc::now();
        let payload = std::mem::take(&mut self.buf);
        let object_key = format!("sessions/{}/{seq}.cast", self.session_id);
        let bucket = self.bucket.clone();
        let publisher = Arc::clone(&self.publisher);
        let session_id = self.session_id;
        let lock = Arc::clone(&self.upload_lock);

        tokio::spawn(async move {
            let _guard = lock.lock().await;
            let size = payload.len() as u64;
            match bucket
                .put_object_with_content_type(
                    &object_key,
                    &payload,
                    "application/octet-stream",
                )
                .await
            {
                Ok(_) => {
                    tracing::info!(session_id, object_key = %object_key, size, "录像切片已上传");
                    let _ = emit_recording_index(
                        publisher.as_ref(),
                        RecordingIndexData {
                            session_id,
                            object_key,
                            size_bytes: size,
                        },
                    )
                    .await;
                }
                Err(err) => {
                    tracing::warn!(session_id, %err, "录像切片上传失败");
                }
            }
        });
    }
}
