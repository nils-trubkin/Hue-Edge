package com.nilstrubkin.hueedge.discovery;

class AuthResponse {
    static class Success{
        String username = "";
        public Success() {
        }
    }

    /*private static class Error{
        String type;
        String address;
        String description;
    }*/

    Success success = new Success();
    //Error error;
    public AuthResponse(Success success) {
        success = new Success();
    }
}
