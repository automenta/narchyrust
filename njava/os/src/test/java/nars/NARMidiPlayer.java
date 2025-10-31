package nars;

import spacegraph.audio.midi.MIDIPiano;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

public class NARMidiPlayer {
	public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException {
		MIDIPiano p = MIDIPiano.piano();
	}
}
