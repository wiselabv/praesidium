//! 构建脚本：编译 proto 生成 gRPC 代码。

fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_prost_build::configure()
        .build_server(false) // Rust 侧仅作 client
        .build_client(true)
        .compile_protos(&["proto/policy.proto"], &["proto"])?;
    Ok(())
}
