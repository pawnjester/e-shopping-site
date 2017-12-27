package co.loystar.loystarbusiness.models.pojos;

/**
 * Created by ordgen on 12/27/17.
 */

public class LoyaltyDeal {
    private int threshold;
    private String reward;
    private String program_type;
    private int total_user_points;
    private int total_user_stamps;

    public LoyaltyDeal(
        int threshold,
        String reward,
        String program_type,
        int total_user_points,
        int total_user_stamps
    ) {
        this.threshold = threshold;
        this.reward = reward;
        this.program_type = program_type;
        this.total_user_points = total_user_points;
        this.total_user_stamps = total_user_stamps;
    }

    public int getTotal_user_stamps() {
        return total_user_stamps;
    }

    public int getTotal_user_points() {
        return total_user_points;
    }

    public String getReward() {
        return reward;
    }

    public int getThreshold() {
        return threshold;
    }

    public String getProgram_type() {
        return program_type;
    }

}
