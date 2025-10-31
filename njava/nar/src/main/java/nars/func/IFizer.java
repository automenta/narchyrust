package nars.func;

/** forms evaluable boolean IF statements from mutex-implying CONJ
 * ex:
 * 	   ((x && y) || (--x && z))    	 |-   if(x,y,z)
 * 	   ((x ==> y) && (--x ==> z))    |-   if(x,y,z)
 * */
public class IFizer {
	//TODO
}
