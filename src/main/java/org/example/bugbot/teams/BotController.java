package org.example.bugbot.teams;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Teams bot webhook (Bot Framework "Activity" protocol, simplified).
 *
 * <p>In a real deployment you register this URL as the messaging endpoint of
 * an Azure Bot resource. Teams POSTs an Activity when a user messages the
 * bot; we reply with a message Activity. Signature/JWT validation is omitted
 * here for brevity — add Bot Framework auth before going to production.
 */
@RestController
@RequestMapping("/api/messages")
public class BotController {

    private final ChatOpsService chatOps;

    public BotController(ChatOpsService chatOps) {
        this.chatOps = chatOps;
    }

    @PostMapping
    public Map<String, Object> onActivity(@RequestBody Map<String, Object> activity) {
        String type = String.valueOf(activity.get("type"));
        if (!"message".equals(type)) {
            // Ignore typing indicators, conversation updates, etc.
            return Map.of("type", "message", "text", "");
        }

        String userText = String.valueOf(activity.getOrDefault("text", ""));
        String reply = chatOps.handle(userText);

        return Map.of(
                "type", "message",
                "textFormat", "markdown",
                "text", reply
        );
    }
}

