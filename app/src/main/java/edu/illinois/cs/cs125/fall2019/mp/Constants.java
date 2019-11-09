package edu.illinois.cs.cs125.fall2019.mp;

/*
 * A handful of classes that hold numeric constants involved in server communication (Checkpoint 4).
 * STOP! Do not modify this file. Changes will be overwritten during official grading.
 */

/**
 * Holds the constant values for the possible game states.
 */
class GameStateID {

    /** The game is ongoing but paused. */
    static final int PAUSED = 0;

    /** The game is running. */
    static final int RUNNING = 1;

    /** The game is over. */
    static final int ENDED = 2;

}

/**
 * Holds constant values for possible player states.
 */
class PlayerStateID {

    /** Invited to the game but not yet joined. */
    static final int INVITED = 0;

    /** Declined the invitation or left the game. */
    static final int REMOVED = 1;

    /** Involved in the game (accepted the invitation) but not currently playing. */
    static final int ACCEPTED = 2;

    /** Playing this game right now. */
    static final int PLAYING = 3;

}

/**
 * Holds team ID constants.
 */
class TeamID {

    /** An observer, not actually playing.
     * When used as the claimant of a target or area, indicates that the objective is uncaptured. */
    static final int OBSERVER = 0;

    /** A player on the red team. */
    static final int TEAM_RED = 1;

    /** A player on the yellow team. */
    static final int TEAM_YELLOW = 2;

    /** A player on the green team. */
    static final int TEAM_GREEN = 3;

    /** A player on the blue team. */
    static final int TEAM_BLUE = 4;

    /** The smallest valid (non-observer) team ID. */
    static final int MIN_TEAM = 1;

    /** The largest valid team ID. */
    static final int MAX_TEAM = 4;

    /** How many teams there can be in a game. */
    static final int NUM_TEAMS = 4;

}
