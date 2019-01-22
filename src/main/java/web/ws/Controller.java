package web.ws;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Controller {
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public Map send(Message message) throws Exception {
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        return new HashMap();
    }
}
