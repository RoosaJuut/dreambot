package roosajuut.dreambot.scriptmain.powerminer;

import org.dreambot.api.input.Keyboard;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.wrappers.widgets.message.Message;

public class TestScript implements ChatListener {

    @Override
    public void onGameMessage(Message message) {
        if (message.getMessage().matches("bones")) {
            Keyboard.type("Yeap");
        }
    }
}
