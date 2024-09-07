# Bitwise AlphaBeta Chess Minimax Algorithm from Scratch
This is a version of the Minimax algorithm written in Java. 

It utilizes bitboards as backing storage, and efficiency is maintained up to a ply of 7. Further improvements are expected to increase it's accuracy and efficiency.

## Distinguishing features
- Bitboard backing storage
  - Uses 64 bit long arrays to represent the entire board. 
    - Six longs per side, for each of the six types of chess pieces.
    - Bitwise or operations can be used to obtain the full board.
  - Leverages efficiency of bitwise operations.
  - Human-readable chess board output for printing/debugging purposes.
- Input/Output Translation
  - Lexicographical analysis from FIDE notation.
  - Parsing and interpretation for legality at the board state.
- Other Features
  - Move virtualization with flags and lock to maintain board state without much overhead.
  - Iterative deepening applied to allow for faster initial output at the start of the game.

All code is original and my own work*, include the FIDE notation conversion (still needs some edge case improvement). 

Regardless, I am happy with the state the project is in, and will feel content once all edge cases has been addressed. 

## Still in progress!

### Primary objectives
- Converting SaveState to a circular based stack with a size equivalent to the max ply.
- Allowing user input for castling and pawn promotion
- Configured OTU for proper ambiguity and expected FIDE notation.



*Note: "my own work" actually means **my own work**. No large language models (*or any chatbots*) were used in the production of the code for this project. This is something that I sadly have to include as many software developers believe passing off LLM generated code as their own isn't morally questionable. I believe these tools have their use case, but I started this project as a means to test myself and learn, something that I'd argue can only be achieved through human trial and error.
