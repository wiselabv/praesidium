package io.github.wiselabv.praesidium.admin.identity.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.ThirdPartyService;
import io.github.wiselabv.praesidium.admin.identity.application.dto.ThirdPartyBindRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.ThirdPartyItem;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第三方登录绑定接口：查看 / 绑定 / 解绑。
 */
@RestController
@RequestMapping("/api/profile/third-party")
public class ThirdPartyController {

    private final ThirdPartyService thirdPartyService;

    public ThirdPartyController(ThirdPartyService thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    @GetMapping
    public ApiResponse<List<ThirdPartyItem>> list(Authentication authentication) {
        return ApiResponse.ok(thirdPartyService.list((Long) authentication.getPrincipal()));
    }

    @PostMapping
    public ApiResponse<ThirdPartyItem> bind(@Valid @RequestBody ThirdPartyBindRequest request,
                                            Authentication authentication) {
        return ApiResponse.ok(thirdPartyService.bind((Long) authentication.getPrincipal(), request));
    }

    @DeleteMapping("/{provider}")
    public ApiResponse<Void> unbind(@PathVariable String provider, Authentication authentication) {
        thirdPartyService.unbind((Long) authentication.getPrincipal(), provider);
        return ApiResponse.ok();
    }
}
