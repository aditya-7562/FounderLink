package com.founderlink.auth.config;

import com.founderlink.auth.exception.UserServiceBadRequestException;
import com.founderlink.auth.exception.UserServiceClientException;
import com.founderlink.auth.exception.UserServiceNotFoundException;
import com.founderlink.auth.exception.UserServiceUnavailableException;
import com.founderlink.auth.exception.UserServiceServerException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(FeignErrorDecoder.class);

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String reason = response.reason() == null ? "No reason provided" : response.reason();

        if (status == 400 || status == 404) {
            log.warn("Feign call failed method={} status={} reason={}", methodKey, status, reason);
        } else {
            log.error("Feign call failed method={} status={} reason={}", methodKey, status, reason);
        }

        return switch (status) {
            case 400 -> new UserServiceBadRequestException(methodKey, reason);
            case 404 -> new UserServiceNotFoundException(methodKey, reason);
            case 503 -> new UserServiceUnavailableException(methodKey, reason);
            default -> status >= 500
                    ? new UserServiceServerException(methodKey, status, reason)
                    : new UserServiceClientException(methodKey, status, reason);
        };
    }
}
