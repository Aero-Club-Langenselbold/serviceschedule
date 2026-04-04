package de.ct.manager.schedule.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import de.ct.manager.schedule.settings.CommonSettingsRepository;
import de.ct.manager.schedule.settings.SettingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlarmPassphraseFilter extends OncePerRequestFilter {

    static final String PASSPHRASE_HEADER = "X-Alarm-Passphrase";
    private static final String ALARM_SAVE_PATH = "/alarm/save";

    private final CommonSettingsRepository commonSettingsRepository;

    public AlarmPassphraseFilter(CommonSettingsRepository commonSettingsRepository) {
        this.commonSettingsRepository = commonSettingsRepository;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod())
                && ALARM_SAVE_PATH.equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String providedPassphrase = request.getHeader(PASSPHRASE_HEADER);

        if (StringUtils.hasText(providedPassphrase) && SecurityContextHolder.getContext().getAuthentication() == null) {
            commonSettingsRepository.findSettingByKey(SettingConstants.ALARM_PASSWORD)
                    .ifPresent(setting -> {
                        if (providedPassphrase.equals(setting.getSettingValue())) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    "alarm-passphrase",
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ALARM_ADMIN")));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.debug("Alarm passphrase authentication successful");
                        } else {
                            log.warn("Invalid alarm passphrase provided");
                        }
                    });
        }

        filterChain.doFilter(request, response);
    }
}
