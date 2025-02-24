package minicraft.core.io;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import minicraft.core.Game;

public class InputHandler implements KeyListener {
	/**
		This class handles key presses; this also implements MouseListener... but I have no idea why.
		It's not used in any way. Ever. As far as I know. Anyway, here are a few tips about this class:

		-This class must instantiated to be used; and it's pretty much always called "input" in the code.

		-The keys are stored in two arrays, one for physical keyboard keys(called "keyboard"), and one for "keys" you make-up (called "keymap") to represent different actions ("virtual keys", you could say).

		-All the Keys in the keyboard array are generated automatically as you ask for them in the code (if they don't already exist), so there's no need to define anything in the keyboard array here.
			--Note: this shouldn't matter, but keys that are not asked for or defined as values here in keymap will be ignored when it comes to key presses.

		-All the "virtual keys" in keymap "map" to a Key object in the keyboard array; that is to say,
			keymap contains a HashMap of string keys, to string values. The keys are the names of the actions,
			and the values are the names of the keyboard keys you physically press to do them.

			-To get whether a key is pressed or not, use input.getKey("key"), where "key" is the name of the key, either physical or virtual. If virtual, all it does is then fetch the corrosponding key from keyboard anyway; but it allows one to change the controls while still making the same key requests in the code.

		-If you want to have multiple possibilities at once when it comes to which key to press to do something, you can! just put a "|" between the mappings. For example, say you wanted both "wasd" and arrow key controls to work, at the same time. How you do this is in the construstor below, where it says "keymap.put(" UP, DOWN, LEFT, and RIGHT.

		-This class supports modifier keys as inputs. To specify a "compound" key (one using modifiders), write "MOD1-MOD2-KEY", that is, "SHIFT-ALT-D" or "ALT-F", with a "-" between the keys. ALWAYS put the actual trigger key last, after all modifiers (the modifiers are: shift, ctrl, and alt).

			--All the magic happens in the getKey() method: If the String keyname input has hyphens("-"), then it's a compound key, and it splits it up between the hyphens. Then, it compares which modifiers are currently being pressed, and which are being requested. Then, a Key object is created, which if the modifiers match, reflects the non-modifier key's "down" and "clicked" values; otherwise they're both false.
			--If a key with no hyph is requested, it skips most of that and just gives you the Key, generating it if needed.

	*/
	public String keyToChange = null; // This is used when listening to change key bindings.
	private String keyChanged = null; // This is used when listening to change key bindings.
	private boolean overwrite = false;

	public String getChangedKey() {
		String key = keyChanged + ";" + keymap.get(keyChanged);
		keyChanged = null;
		return key;
	}

	private static HashMap<Integer, String> keyNames = new HashMap<>();
	static {
		Field[] keyEventFields = KeyEvent.class.getFields();
		ArrayList<Field> keyConstants = new ArrayList<>();
		for (Field field: keyEventFields) {
			if (field.getName().contains("VK_") && (field.getType().getName().equals(int.class.getName())))
				keyConstants.add(field);
		}

		for (Field keyConst: keyConstants) {
			String name = keyConst.getName();
			name = name.substring(3); // Removes the "VK_"
			try {
				keyNames.put(((Integer)keyConst.get(0)), name);
			} catch(IllegalAccessException ignored) {}
		}

		// For compatibility becuase I'm lazy. :P
		keyNames.put(KeyEvent.VK_BACK_SPACE, "BACKSPACE");
		keyNames.put(KeyEvent.VK_CONTROL, "CTRL");
	}

	private HashMap<String, String> keymap; // The symbolic map of actions to physical key names.
	private HashMap<String, Key> keyboard; // The actual map of key names to Key objects.
	private String lastKeyTyped = ""; // Used for things like typing world names.
	private String keyTypedBuffer = ""; // Used to store the last key typed before putting it into the main var during tick().

	public InputHandler() {
		keymap = new LinkedHashMap<>(); // Stores custom key name with physical key name in keyboard.
		keyboard = new HashMap<>(); // Stores physical keyboard keys; auto-generated :D

		initKeyMap(); // This is seperate so I can make a "restore defaults" option.

		// I'm not entirely sure if this is necessary... but it doesn't hurt.
		keyboard.put("SHIFT", new Key(true));
		keyboard.put("CTRL", new Key(true));
		keyboard.put("ALT", new Key(true));
	}
	public InputHandler(Component inputSource) {
		this();
		inputSource.addKeyListener(this); // Add key listener to game
	}

	private void initKeyMap() {
		keymap.put("MOVE-UP", "UP|W");
		keymap.put("MOVE-DOWN", "DOWN|S");
		keymap.put("MOVE-LEFT", "LEFT|A");
		keymap.put("MOVE-RIGHT", "RIGHT|D");

		keymap.put("CURSOR-UP", "UP");
		keymap.put("CURSOR-DOWN", "DOWN");
		keymap.put("CURSOR-LEFT", "LEFT");
		keymap.put("CURSOR-RIGHT", "RIGHT");

		keymap.put("SELECT", "ENTER");
		keymap.put("EXIT", "ESCAPE");

		keymap.put("QUICKSAVE", "R"); // Saves the game while still playing

		keymap.put("ATTACK", "C|SPACE|ENTER"); // Attack action references "C" key
		keymap.put("MENU", "X|E"); // And so on... menu does various things.
		keymap.put("CRAFT", "Z|SHIFT-E"); // Open/close personal crafting window.
		keymap.put("PICKUP", "V|P"); // Pickup torches / furniture; this replaces the power glove.
		keymap.put("DROP-ONE", "Q"); // Drops the item in your hand, or selected in your inventory, by ones; it won't drop an entire stack
		keymap.put("DROP-STACK", "SHIFT-Q"); // Drops the item in your hand, or selected in your inventory, entirely; even if it's a stack.

		// Toggle inventory searcher bar
		keymap.put("SEARCHER-BAR", "SHIFT-F");

		// Seek for next/previous match in inventory searcher bar
		keymap.put("PAGE-UP", "PAGE_UP");
		keymap.put("PAGE-DOWN", "PAGE_DOWN");

		keymap.put("PAUSE", "ESCAPE"); // Pause the Game.

		keymap.put("SURVIVAL=debug", "SHIFT-S|SHIFT-1");
		keymap.put("CREATIVE=debug", "SHIFT-C|SHIFT-2");

		keymap.put("POTIONEFFECTS", "P"); // Toggle potion effect display
		keymap.put("SIMPPOTIONEFFECTS", "O"); // Whether to simplify the potion effect display
		keymap.put("TOGGLEHUD", "F1"); // Whether to hide hide GUI
		keymap.put("EXPANDQUESTDISPLAY", "L"); // Expands the quest display
		keymap.put("TOGGLEDEBUG", "F3"); // Toggle fps display
		keymap.put("INFO", "SHIFT-I"); // Toggle player stats display

		keymap.put("FULLSCREEN", "F11");
	}

	public void resetKeyBindings() {
		keymap.clear();
		initKeyMap();
	}

	/** Processes each key one by one, in keyboard. */
	public void tick() {
		lastKeyTyped = keyTypedBuffer;
		keyTypedBuffer = "";
		synchronized ("lock") {
			for (Key key: keyboard.values())
				key.tick(); // Call tick() for each key.
		}
	}

	/** For game controllers. */
	public boolean isClicked(String key, ControllerHandler controlInput) {
		return controlInput.isKeyPressed(key) || getKey(key).clicked;
	}
	public boolean isHeld(String key, ControllerHandler controlInput) {
		return controlInput.isKeyDown(key) || getKey(key).down;
	}

	// The Key class.
	public class Key {
		// presses = how many times the Key has been pressed.
		// absorbs = how many key presses have been processed.
		private int presses, absorbs;
		// down = if the key is currently physically being held down.
		// clicked = if the key is still being processed at the current tick.
		public boolean down, clicked;
		// sticky = true if presses reaches 3, and the key continues to be held down.
		private boolean sticky;

		boolean stayDown;

		public Key() { this(false); }
		public Key(boolean stayDown) {
			this.stayDown = stayDown;
		}

		/** toggles the key down or not down. */
		public void toggle(boolean pressed) {
			down = pressed; // Set down to the passed in value; the if statement is probably unnecessary...
			if (pressed && !sticky) presses++; // Add to the number of total presses.
		}

		/** Processes the key presses. */
		public void tick() {
			if (absorbs < presses) { // If there are more key presses to process...
				absorbs++; // Process them!
				if(presses - absorbs > 3) absorbs = presses - 3;
				clicked = true; // Make clicked true, since key presses are still being processed.
			} else { // All key presses so far for this key have been processed.
				if (!sticky) sticky = presses > 3;
				else sticky = down;
				clicked = sticky; // Set clicked to false, since we're done processing; UNLESS the key has been held down for a bit, and hasn't yet been released.

				// Reset the presses and absorbs, to ensure they don't get too high, or something:
				presses = 0;
				absorbs = 0;
			}
		}

		public void release() {
			down = false;
			clicked = false;
			presses = 0;
			absorbs = 0;
			sticky = false;
		}

		// Custom toString() method, I used it for debugging.
		public String toString() {
			return "down:" + down + "; clicked:" + clicked + "; presses=" + presses + "; absorbs=" + absorbs;
		}
	}

	/** This is used to stop all of the actions when the game is out of focus. */
	public void releaseAll() {
		for (Key key: keyboard.values().toArray(new Key[0])) {
			key.release();
		}
	}

	/// This is meant for changing the default keys. Call it from the options menu, or something.
	public void setKey(String keymapKey, String keyboardKey) {
		if (keymapKey != null && keymap.containsKey(keymapKey) && (!keymapKey.contains("=debug") || Game.debug)) // The keyboardKey can be null, I suppose, if you want to disable a key...
			keymap.put(keymapKey, keyboardKey);
	}

	/** Simply returns the mapped value of key in keymap. */
	public String getMapping(String actionKey) {
		actionKey = actionKey.toUpperCase();
		if (keymap.containsKey(actionKey))
			return keymap.get(actionKey).replace("|", "/");

		return "NO_KEY";
	}

	/// THIS is pretty much the only way you want to be interfacing with this class; it has all the auto-create and protection functions and such built-in.
	public Key getKey(String keytext) { return getKey(keytext, true); }
	private Key getKey(String keytext, boolean getFromMap) {
		// If the passed-in key is blank, or null, then return null.
		if (keytext == null || keytext.length() == 0) return new Key();

		Key key; // Make a new key to return at the end
		keytext = keytext.toUpperCase(java.util.Locale.ENGLISH); // Prevent errors due to improper "casing"

		synchronized ("lock") {
			// This should never be run, actually, b/c the "=debug" isn't used in other places in the code.
			if(keymap.containsKey(keytext+"=debug")) {
				if(!Game.debug) return new Key();
				else keytext += "=debug";
			}

			if(getFromMap) { // If false, we assume that keytext is a physical key.
				// If the passed-in key equals one in keymap, then replace it with it's match, a key in keyboard.
				if (keymap.containsKey(keytext))
					keytext = keymap.get(keytext); // Converts action name to physical key name
			}
		}

		String fullKeytext = keytext;

		if (keytext.contains("|")) {
			/// Multiple key possibilities exist for this action; so, combine the results of each one!
			key = new Key();
			for (String keyposs: keytext.split("\\|")) { // String.split() uses regex, and "|" is a special character, so it must be escaped; but the backslash must be passed in, so it needs escaping.
				Key aKey = getKey(keyposs, false); // This time, do NOT attempt to fetch from keymap.

				// It really does combine using "or":
				key.down = key.down || aKey.down;
				key.clicked = key.clicked || aKey.clicked;
			}
			return key;
		}

		synchronized ("lock") {
			if (keytext.contains("-")) // Truncate compound keys to only the base key, no modifiers
				keytext = keytext.substring(keytext.lastIndexOf("-")+1);

			if (keyboard.containsKey(keytext))
				key = keyboard.get(keytext); // Gets the key object from keyboard, if if exists.
			else {
				// If the specified key does not yet exist in keyboard, then create a new Key, and put it there.
				key = new Key(); // Make new key
				keyboard.put(keytext, key); // Add it to keyboard

				//if(Game.debug) System.out.println("Added new key: \'" + keytext + "\'"); //log to console that a new key was added to the keyboard
			}
		} // "key" has been set to the appropriate key Object.

		keytext = fullKeytext;

		if (keytext.equals("SHIFT") || keytext.equals("CTRL") || keytext.equals("ALT"))
			return key; // Nothing more must be done with modifier keys.

		boolean foundS = false, foundC = false, foundA = false;
		if (keytext.contains("-")) {
			for (String keyname: keytext.split("-")) {
				if (keyname.equals("SHIFT")) foundS = true;
				if (keyname.equals("CTRL")) foundC = true;
				if (keyname.equals("ALT")) foundA = true;
			}
		}
		boolean modMatch =
		  getKey("shift").down == foundS &&
		  getKey("ctrl").down == foundC &&
		  getKey("alt").down == foundA;

		if (keytext.contains("-")) { // We want to return a compound key, but still care about the trigger key.
			Key mainKey = key; // Move the fetched key to a different variable

			key = new Key(); // Set up return key to have proper values
			key.down = modMatch && mainKey.down;
			key.clicked = modMatch && mainKey.clicked;
		}
		else if (!modMatch) key = new Key();

		//if(key.clicked && Game.debug) System.out.println("Processed key: " + keytext + " is clicked; tickNum=" + ticks);

		return key; // Return the Key object.
	}

	/// This method provides a way to press physical keys without actually generating a key event.
	/*public void pressKey(String keyname, boolean pressed) {
		Key key = getPhysKey(keyname);
		key.toggle(pressed);
		//System.out.println("Key " + keyname + " is clicked: " + getPhysKey(keyname).clicked);
	}*/

	public ArrayList<String> getAllPressedKeys() {
		ArrayList<String> keyList = new ArrayList<>(keyboard.size());

		for (Entry<String, Key> entry : keyboard.entrySet()) {
			if (entry.getValue().down) {
				keyList.add(entry.getKey());
			}
		}

		return keyList;
	}

	/// This gets a key from key text, w/o adding to the key list.
	private Key getPhysKey(String keytext) {
		keytext = keytext.toUpperCase();

		if (keyboard.containsKey(keytext))
			return keyboard.get(keytext);
		else {
			//System.out.println("UNKNOWN KEYBOARD KEY: " + keytext); // it's okay really; was just checking
			return new Key(); // Won't matter where I'm calling it.
		}
	}

	// Called by KeyListener Event methods, below. Only accesses keyboard Keys.
	private void toggle(int keycode, boolean pressed) {
		String keytext = "NO_KEY";

		if (keyNames.containsKey(keycode))
			keytext = keyNames.get(keycode);
		else {
			Logger.tag("INPUT").error("Could not find keyname for keycode \"" + keycode + "\"");
			return;
		}

		keytext = keytext.toUpperCase();

		//System.out.println("Interpreted key press: " + keytext);

		//System.out.println("Toggling " + keytext + " key (keycode " + keycode + ") to "+pressed+".");
		if( pressed && keyToChange != null && !isMod(keytext) ) {
			keymap.put(keyToChange, ( overwrite ? "" : keymap.get(keyToChange) + "|" ) + getCurModifiers() + keytext);
			keyChanged = keyToChange;
			keyToChange = null;
			return;
		}
		getPhysKey(keytext).toggle(pressed);
	}

	private static boolean isMod(String keyname) {
		keyname = keyname.toUpperCase();
		return keyname.equals("SHIFT") || keyname.equals("CTRL") || keyname.equals("ALT");
	}

	private String getCurModifiers() {
		return (getKey("ctrl").down ? "CTRL-" : "") +
				(getKey("alt").down ? "ALT-" : "") +
				(getKey("shift").down ? "SHIFT-" : "");
	}

	/** Used by Save.java, to save user key preferences. */
	public String[] getKeyPrefs() {
		ArrayList<String> keystore = new ArrayList<>(); // Make a list for keys

		for (String keyname: keymap.keySet()) // Go though each mapping
			if(!keyname.contains("=debug") || Game.debug)
				keystore.add(keyname + ";" + keymap.get(keyname)); // Add the mapping values as one string, seperated by a semicolon.

		return keystore.toArray(new String[0]); // Return the array of encoded key preferences.
	}


	public void changeKeyBinding(String actionKey) {
		keyToChange = actionKey.toUpperCase();
		overwrite = true;
	}

	public void addKeyBinding(String actionKey) {
		keyToChange = actionKey.toUpperCase();
		overwrite = false;
	}

	/// Event methods, many to satisfy interface requirements...
	public void keyPressed(KeyEvent ke) { toggle(ke.getExtendedKeyCode(), true); }
	public void keyReleased(KeyEvent ke) { toggle(ke.getExtendedKeyCode(), false); }
	public void keyTyped(KeyEvent ke) {
		// Stores the last character typed
		keyTypedBuffer = String.valueOf(ke.getKeyChar());
	}

	private static final String control = "\\p{Print}"; // Should match only printable characters.
	public String addKeyTyped(String typing, @Nullable String pattern) {
		if (lastKeyTyped.length() > 0) {
			String letter = lastKeyTyped;
			lastKeyTyped = "";
			if ( letter.matches(control) && (pattern == null || letter.matches(pattern)) )
				typing += letter;
		}

		if (getKey("backspace").clicked && typing.length() > 0) {
			// Backspace counts as a letter itself, but we don't have to worry about it if it's part of the regex.
			typing = typing.substring(0, typing.length()-1);
		}

		return typing;
	}
}
