package com.example.sharded_wallet21.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.example.sharded_wallet21.repositories.WalletRepository;

import lombok.RequiredArgsConstructor;

/** Ownership checks used from @PreAuthorize expressions and controllers. */
@Component("authz")
@RequiredArgsConstructor
public class Authz {
    private final WalletRepository walletRepository;

    public Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Not authenticated");
        }
        return Long.valueOf(jwt.getSubject());
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isSelfOrAdmin(Long userId) {
        return isAdmin() || currentUserId().equals(userId);
    }

    public boolean ownsWallet(Long walletId) {
        if (isAdmin()) {
            return true;
        }
        if (walletId == null) {
            return false;
        }
        Long me = currentUserId();
        return walletRepository.findById(walletId).map(w -> me.equals(w.getUserId())).orElse(false);
    }
}
