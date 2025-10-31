package nars.func.logic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

/**
 * http:
 * http:
 */
class TestProgol {
    @Test
    void testAnimals() throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        /*
        % Mode declarations
        :- modeh(1,class(+animal,#class))?
        :- modeb(1,has_milk(+animal))?
        :- modeb(1,has_gills(+animal))?
        :- modeb(1,has_covering(+animal,#covering))?
        :- modeb(1,has_legs(+animal,#nat))?
        :- modeb(1,homeothermic(+animal))?
        :- modeb(1,has_eggs(+animal))?
        :- modeb(1,not has_milk(+animal))?
        :- modeb(1,not has_gills(+animal))?
        :- modeb(*,habitat(+animal,#habitat))?
        :- modeh(1,false)?
        :- modeb(1,class(+animal,#class))?
         */

        
        n.input(
                """
                        animal(dog).  animal(dolphin).  animal(platypus).  animal(bat).
                        animal(trout).  animal(herring).  animal(shark). animal(eel).
                        animal(lizard).  animal(crocodile).  animal(t_rex).  animal(turtle).
                        animal(snake).  animal(eagle).  animal(ostrich).  animal(penguin).
                        species(mammal).  species(fish).  species(reptile).  species(bird).
                        covering(hair).  covering(none).  covering(scales).  covering(feathers).
                        habitat(land).  habitat(water).  habitat(air).  habitat(caves).""");

        
        n.input(
                """
                        class(dog,mammal).
                        class(dolphin,mammal).
                        class(platypus,mammal).
                        class(bat,mammal).
                        class(trout,fish).
                        class(herring,fish).
                        class(shark,fish).
                        class(eel,fish).
                        class(lizard,reptile).
                        class(crocodile,reptile).
                        class(t_rex,reptile).
                        class(snake,reptile).
                        class(turtle,reptile).
                        class(eagle,bird).
                        class(ostrich,bird).
                        class(penguin,bird).""");

        
        n.input(
                """
                        --(class(#X,mammal) && class(#X,fish)).
                        --(class(#X,mammal) && class(#X,reptile)).
                        --(class(#X,mammal) && class(#X,bird)).
                        --(class(#X,fish) && class(#X,reptile)).
                        --(class(#X,fish) && class(#X,bird)).
                        --(class(#X,reptile) && class(#X,bird)).
                        --class(eagle,reptile).
                        --class(trout,mammal).
                        --class(herring,mammal).
                        --class(shark,mammal).
                        --class(lizard,mammal).
                        --class(crocodile,mammal).
                        --class(t_rex,mammal).
                        --class(turtle,mammal).
                        --class(eagle,mammal).
                        --class(ostrich,mammal).
                        --class(penguin,mammal).
                        --class(dog,fish).
                        --class(dolphin,fish).
                        --class(platypus,fish).
                        --class(bat,fish).
                        --class(lizard,fish).
                        --class(crocodile,fish).
                        --class(t_rex,fish).
                        --class(turtle,fish).
                        --class(eagle,fish).
                        --class(ostrich,fish).
                        --class(penguin,fish).
                        --class(dog,reptile).
                        --class(dolphin,reptile).
                        --class(platypus,reptile).
                        --class(bat,reptile).
                        --class(trout,reptile).
                        --class(herring,reptile).
                        --class(shark,reptile).
                        --class(eagle,reptile).
                        --class(ostrich,reptile).
                        --class(penguin,reptile).
                        --class(dog,bird).
                        --class(dolphin,bird).
                        --class(platypus,bird).
                        --class(bat,bird).
                        --class(trout,bird).
                        --class(herring,bird).
                        --class(shark,bird).
                        --class(lizard,bird).
                        --class(crocodile,bird).
                        --class(t_rex,bird).
                        --class(turtle,bird).""");

        
        n.input(
                """
                        has_covering(dog,hair).
                        has_covering(dolphin,none).
                        has_covering(platypus,hair).
                        has_covering(bat,hair).
                        has_covering(trout,scales).
                        has_covering(herring,scales).
                        has_covering(shark,none).
                        has_covering(eel,none).
                        has_covering(lizard,scales).
                        has_covering(crocodile,scales).
                        has_covering(t_rex,scales).
                        has_covering(snake,scales).
                        has_covering(turtle,scales).
                        has_covering(eagle,feathers).
                        has_covering(ostrich,feathers).
                        has_covering(penguin,feathers).
                        has_legs(dog,4).
                        has_legs(dolphin,0).
                        has_legs(platypus,2).
                        has_legs(bat,2).
                        has_legs(trout,0).
                        has_legs(herring,0).
                        has_legs(shark,0).
                        has_legs(eel,0).
                        has_legs(lizard,4).
                        has_legs(crocodile,4).
                        has_legs(t_rex,4).
                        has_legs(snake,0).
                        has_legs(turtle,4).
                        has_legs(eagle,2).
                        has_legs(ostrich,2).
                        has_legs(penguin,2).
                        has_milk(dog).
                        has_milk(dolphin).
                        has_milk(bat).
                        has_milk(platypus).
                        homeothermic(dog).
                        homeothermic(dolphin).
                        homeothermic(platypus).
                        homeothermic(bat).
                        homeothermic(eagle).
                        homeothermic(ostrich).
                        homeothermic(penguin).
                        habitat(dog,land).
                        habitat(dolphin,water).
                        habitat(platypus,water).
                        habitat(bat,air).
                        habitat(bat,caves).
                        habitat(trout,water).
                        habitat(herring,water).
                        habitat(shark,water).
                        habitat(eel,water).
                        habitat(lizard,land).
                        habitat(crocodile,water).
                        habitat(crocodile,land).
                        habitat(t_rex,land).
                        habitat(snake,land).
                        habitat(turtle,water).
                        habitat(eagle,air).
                        habitat(eagle,land).
                        habitat(ostrich,land).
                        habitat(penguin,water).
                        has_eggs(platypus).
                        has_eggs(trout).
                        has_eggs(herring).
                        has_eggs(shark).
                        has_eggs(eel).
                        has_eggs(lizard).
                        has_eggs(crocodile).
                        has_eggs(t_rex).
                        has_eggs(snake).
                        has_eggs(turtle).
                        has_eggs(eagle).
                        has_eggs(ostrich).
                        has_eggs(penguin).
                        has_gills(trout).
                        has_gills(herring).
                        has_gills(shark).
                        has_gills(eel).
                        animal(cat). animal(dragon).
                        animal(girl).
                        animal(boy).
                        has_milk(cat).
                        homeothermic(cat).
                        """);

        n.freqRes.set(0.25f);
        //n.stats(System.out);
        
        n.run(100);
        //n.stats(System.out);

    }
}