package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;

public class ClientTickEvent extends EventBase {
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public static class Pre extends ClientTickEvent {
		
	}
	
    public static class Post extends ClientTickEvent {
		
	}

}
