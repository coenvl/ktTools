package com.ktipr.kttools.perfecttune;

import org.bukkit.Note;

public class KtNote extends Note {

	public KtNote(byte raw) {
		super(raw);
	}
	
	public KtNote(byte octave, Tone note, boolean sharped) {
		super(octave, note, sharped);
	}
	
	@Override
	public String toString() {
		return "" + (getOctave() + 1) + getTone() + (isSharped() ? "#" : "") + " (" + this.getId() + ")";
	}
}