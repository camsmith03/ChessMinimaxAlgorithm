package com.github.camsmith03;

public class Evaluator {

    /*
    GOALS:
        - evaluateMoves(MoveList, boolean buildMax) -> Heap
        - evaluate(Bitboard) -> int

    PLAN:

        We need to organize the structure of this class to prevent confusion.

        First, given a bitboard, this must be able to compute an int value corresponding with some predefined weights.

        Considerations for different weights:
            - Piece value
            - Control over center
            - Moves that put enemy king in check
            - Piece captures
            - Castling weight
            etc...

        These weights should be adjustable so that small tweaks can be made and tested to find the perfect configuration
        of them all.


        Second, we have to determine who will be building the heap of evaluated integers.

            It could be...
                - Evaluator -> this is a valid approach, but it also introduces the need to interface between MoveList.

                - MoveList -> this would allow MoveList to build a heap, although, would it take away from MoveList
                              being solely a data structure

                - Minimax  -> don't really know if its the best or worst approach as of right now.


            We know our input will be a MoveList. This puts MoveList as a valid contender for offering the best
            solution. We can create a Heap data structure designed for KVPairs of Moves and Integers, then interface
            with Evaluator for every MoveList element until they are all appended to the Heap.

            Note, we designed MoveList to be an array of LinkedLists, which has the benefit of separating the piece
            moves by type. Knowing this, moves that tend to lead to higher or lower values based on type can be computed
            before others to speed up Heap construction. This efficiency boost would probably be minimal at best, which
            puts our design for MoveList at a disadvantage.

            MoveList does have the ability to quickly clear a list. This means we really only need one object to
            represent it.

            Think about how this would work:
                - including a tempMove() method to bitboard (update boards but keep tempBoards as copy),
                - passing bitboard to evaluator,
                - evaluating boards,
                - undoing said move with an undoMove() method (restoring copy).

            Is this the best approach? Is there a better alternative? What about Board?

            Should Evaluator be passed a Move instead?
                No, we want the evaluation to be done on static boards.

            This is a tough one

     */


}
