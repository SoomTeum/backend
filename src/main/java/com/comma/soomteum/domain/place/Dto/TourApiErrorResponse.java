package com.comma.soomteum.domain.place.Dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "OpenAPI_ServiceResponse")
public class TourApiErrorResponse {

    @JsonProperty("cmmMsgHeader")
    private CmmMsgHeader cmmMsgHeader;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CmmMsgHeader {
        private String errMsg;            // e.g. "SERVICE ERROR"
        private String returnAuthMsg;     // e.g. SERVICE_KEY_IS_NOT_REGISTERED_ERROR
        private String returnReasonCode;  // e.g. "30"
    }

    // ===== 편의 메서드들 ===== //
    @JsonIgnore
    public boolean hasError() {
        return cmmMsgHeader != null && (
                notBlank(cmmMsgHeader.getReturnAuthMsg()) ||
                        notBlank(cmmMsgHeader.getReturnReasonCode()) ||
                        "SERVICE ERROR".equalsIgnoreCase(nullToEmpty(cmmMsgHeader.getErrMsg()))
        );
    }

    @JsonIgnore
    public String getErrorCode() { // = returnReasonCode
        return cmmMsgHeader != null ? cmmMsgHeader.getReturnReasonCode() : null;
    }

    @JsonIgnore
    public String getAuthMsg() { // = returnAuthMsg
        return cmmMsgHeader != null ? cmmMsgHeader.getReturnAuthMsg() : null;
    }

    @JsonIgnore
    public String getErrorMsg() { // = errMsg
        return cmmMsgHeader != null ? cmmMsgHeader.getErrMsg() : null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
