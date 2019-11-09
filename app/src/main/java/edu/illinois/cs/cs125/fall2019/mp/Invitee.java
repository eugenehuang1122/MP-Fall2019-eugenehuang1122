package edu.illinois.cs.cs125.fall2019.mp;

/**
 * Represents a person invited to a game for the game setup activity.
 */
public final class Invitee {

    /** Which team the player is on, one of the TeamID constants. */
    private int teamId;

    /** The invitee's email address. */
    private String email;

    /**
     * Creates a new Invitee (player record).
     * @param setEmail email address
     * @param setTeamId TeamID code for the user's role
     */
    public Invitee(final String setEmail, final int setTeamId) {
        email = setEmail;
        teamId = setTeamId;
    }

    /**
     * Gets the invitee's team/role ID.
     * @return the TeamID code for the role the user would have in the game
     */
    public int getTeamId() {
        return teamId;
    }

    /**
     * Sets the invitee's team/role ID.
     * @param newTeamId the TeamID code for the new role the user would have in the game
     */
    public void setTeamId(final int newTeamId) {
        teamId = newTeamId;
    }

    /**
     * Gets the email address.
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     * @param newEmail the new email address
     */
    public void setEmail(final String newEmail) {
        email = newEmail;
    }
}
