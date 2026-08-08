package com.heartsync.auth;

import com.heartsync.common.constants.ApiConstants;
import com.heartsync.common.enums.Gender;
import com.heartsync.common.util.StringUtils;

public class TestCommonLibrary {
    public static void main(String[] args) {
        

        System.out.println(ApiConstants.API_V1);

        System.out.println(Gender.MALE);

        System.out.println(StringUtils.trim("   HeartSync   "));
    }
}
