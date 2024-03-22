package com.five.pojo.pojo;

import jakarta.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineFiveActor {
    private String role;
    private Session session;

    public void closeSession() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            System.err.println("Error occurred while closing the session: " + e.getMessage());
        }
    }

}
