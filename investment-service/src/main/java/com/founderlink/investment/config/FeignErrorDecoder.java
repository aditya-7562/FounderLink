package com.founderlink.investment.config;

import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.exception.StartupServiceServerException;
import com.founderlink.investment.exception.StartupServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String reason = response.reason() == null ? "No reason provided" : response.reason();

        if (status == 400 || status == 404 || status == 403) {
            log.warn("Feign call failed method={} status={} reason={}", methodKey, status, reason);
        } else {
            log.error("Feign call failed method={} status={} reason={}", methodKey, status, reason);
        }

        return switch (status) {
            case 403 -> new ForbiddenAccessException(reason);
            case 404 -> new StartupNotFoundException(reason);
            case 503 -> new StartupServiceUnavailableException(methodKey, reason);
            default  -> status >= 500
                    ? new StartupServiceServerException(methodKey, status, reason)
                    : new RuntimeException("Feign error [" + status + "] on " + methodKey + ": " + reason);
        };
    }
}
