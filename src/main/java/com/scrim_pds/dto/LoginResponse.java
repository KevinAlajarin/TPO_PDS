package com.scrim_pds.dto;

import com.scrim_pds.model.User;

public class LoginResponse {
    private String token;
    private UserData user;

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = new UserData(user);
    }

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    /**
     * Clase interna para filtrar los datos del usuario que devolvemos.
     * NUNCA devolver el passwordHash.
     */
    public static class UserData {
        private String id;
        private String username;
        private String email;
        private String rol;

        public UserData(User user) {
            this.id = user.getId().toString();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.rol = user.getRol().name();
        }

        // Getters
        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRol() { return rol; }
    }
}