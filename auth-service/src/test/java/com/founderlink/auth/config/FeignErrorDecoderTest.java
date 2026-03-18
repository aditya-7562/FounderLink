package com.founderlink.auth.config;

import com.founderlink.auth.exception.UserServiceBadRequestException;
import com.founderlink.auth.exception.UserServiceClientException;
import com.founderlink.auth.exception.UserServiceNotFoundException;
import com.founderlink.auth.exception.UserServiceServerException;
import com.founderlink.auth.exception.UserServiceUnavailableException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FeignErrorDecoderTest {

    private final FeignErrorDecoder decoder = new FeignErrorDecoder();

    @Test
    void decodeShouldReturnBadRequestExceptionFor400() {
        Exception exception = decoder.decode("UserClient#createUser", buildResponse(400, "Bad Request"));

        assertThat(exception).isInstanceOf(UserServiceBadRequestException.class);
        assertThat(exception.getMessage()).contains("method=UserClient#createUser").contains("status=400");
    }

    @Test
    void decodeShouldReturnNotFoundExceptionFor404() {
        Exception exception = decoder.decode("UserClient#createUser", buildResponse(404, "Not Found"));

        assertThat(exception).isInstanceOf(UserServiceNotFoundException.class);
        assertThat(exception.getMessage()).contains("method=UserClient#createUser").contains("status=404");
    }

    @Test
    void decodeShouldReturnServiceUnavailableExceptionFor503() {
        Exception exception = decoder.decode("UserClient#createUser", buildResponse(503, "Service Unavailable"));

        assertThat(exception).isInstanceOf(UserServiceUnavailableException.class);
        assertThat(exception.getMessage()).contains("method=UserClient#createUser").contains("status=503");
    }

    @Test
    void decodeShouldReturnServerExceptionForUnexpected5xxStatus() {
        Exception exception = decoder.decode("UserClient#createUser", buildResponse(502, "Bad Gateway"));

        assertThat(exception).isInstanceOf(UserServiceServerException.class);
        assertThat(((UserServiceClientException) exception).getStatus()).isEqualTo(502);
        assertThat(exception.getMessage()).contains("method=UserClient#createUser").contains("status=502");
    }

    private Response buildResponse(int status, String reason) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/users/internal",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );

        return Response.builder()
                .status(status)
                .reason(reason)
                .request(request)
                .headers(Collections.emptyMap())
                .build();
    }
}
