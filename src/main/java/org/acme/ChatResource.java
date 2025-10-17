package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/chat")
public class ChatResource {

    @Inject
    ChatService chatService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(ChatRequest request) {
        String response = chatService.chat(request.message());
        return new ChatResponse(response);
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String response) {}
}
