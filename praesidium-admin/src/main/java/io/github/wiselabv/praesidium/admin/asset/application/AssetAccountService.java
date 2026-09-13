package io.github.wiselabv.praesidium.admin.asset.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetAccountItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetAccountRequest;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.CredentialRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Credential;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产账号应用服务：账号 CRUD（归属资产校验 + 凭据绑定）。
 */
@Service
public class AssetAccountService {

    private final AssetAccountRepository accountRepository;
    private final AssetRepository assetRepository;
    private final CredentialRepository credentialRepository;

    public AssetAccountService(AssetAccountRepository accountRepository,
                               AssetRepository assetRepository,
                               CredentialRepository credentialRepository) {
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
        this.credentialRepository = credentialRepository;
    }

    /** 某资产下的账号清单（资产详情用，不分页） */
    @Transactional(readOnly = true)
    public List<AssetAccountItem> listByAsset(Long assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        List<AssetAccount> accounts = accountRepository.findByAssetId(assetId);
        Map<Long, String> credentialNames = credentialNames(accounts);
        return accounts.stream()
                .map(a -> AssetAccountItem.from(a, null, credentialNames.get(a.getCredentialId())))
                .toList();
    }

    /** 全局账号分页清单 */
    @Transactional(readOnly = true)
    public PageResponse<AssetAccountItem> listPage(String keyword, int page, int size) {
        List<AssetAccount> accounts = accountRepository.findPage(keyword, (page - 1) * size, size);
        long total = accountRepository.countPage(keyword);
        Map<Long, String> assetNames = assetRepository.findByIds(
                        accounts.stream().map(AssetAccount::getAssetId).distinct().toList())
                .stream().collect(Collectors.toMap(Asset::getId, Asset::getName));
        Map<Long, String> credentialNames = credentialNames(accounts);
        List<AssetAccountItem> items = accounts.stream()
                .map(a -> AssetAccountItem.from(a, assetNames.get(a.getAssetId()),
                        credentialNames.get(a.getCredentialId())))
                .toList();
        return PageResponse.of(items, total, page, size);
    }

    @Transactional
    public AssetAccountItem create(Long assetId, AssetAccountRequest request) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        String name = request.name().trim();
        if (accountRepository.existsByAssetAndName(assetId, name)) {
            throw new BizException(ApiErrorCode.ASSET_ACCOUNT_EXISTS, "该资产下已存在同名账号");
        }
        Long credentialId = resolveCredential(request.credentialId());
        AssetAccount account = AssetAccount.create(assetId, name, request.type().trim(),
                request.privileged(), request.source(), credentialId, request.enabled());
        accountRepository.save(account);
        return AssetAccountItem.from(account, asset.getName(), credentialName(credentialId));
    }

    @Transactional
    public AssetAccountItem update(Long id, AssetAccountRequest request) {
        AssetAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_ACCOUNT_NOT_FOUND, "资产账号不存在"));
        String name = request.name().trim();
        if (accountRepository.existsByAssetAndNameExcludingId(account.getAssetId(), name, id)) {
            throw new BizException(ApiErrorCode.ASSET_ACCOUNT_EXISTS, "该资产下已存在同名账号");
        }
        Long credentialId = resolveCredential(request.credentialId());
        account.update(name, request.type().trim(), request.privileged(), request.source(),
                credentialId, request.enabled());
        accountRepository.save(account);
        String assetName = assetRepository.findById(account.getAssetId()).map(Asset::getName).orElse(null);
        return AssetAccountItem.from(account, assetName, credentialName(credentialId));
    }

    @Transactional
    public void delete(Long id) {
        AssetAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_ACCOUNT_NOT_FOUND, "资产账号不存在"));
        accountRepository.delete(account);
    }

    /** 凭据绑定校验：id 给出则必须存在 */
    private Long resolveCredential(Long credentialId) {
        if (credentialId == null) {
            return null;
        }
        credentialRepository.findById(credentialId)
                .orElseThrow(() -> new BizException(ApiErrorCode.CREDENTIAL_NOT_FOUND, "凭据不存在"));
        return credentialId;
    }

    private Map<Long, String> credentialNames(List<AssetAccount> accounts) {
        List<Long> ids = accounts.stream().map(AssetAccount::getCredentialId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return credentialRepository.findByIds(ids).stream()
                .collect(Collectors.toMap(Credential::getId, Credential::getName));
    }

    private String credentialName(Long credentialId) {
        if (credentialId == null) {
            return null;
        }
        return credentialRepository.findById(credentialId).map(Credential::getName).orElse(null);
    }
}
