import com.safenetinc.luna.LunaSlotManager;

public class MiscSetPIN {

  // Configure these as required.
  private static final int slot = 0;
  private static final String password = "userpin";
  private static final String newPassword = "newuserpin";

  private static final LunaSlotManager lsm = LunaSlotManager.getInstance();

  public static void main(String[] args) throws Exception {
    System.out.println("Logging into slot=" + slot);
    lsm.login(slot, password);

    System.out.println("Changing the partition password from '" + password + "' to '" + newPassword + "'");
    lsm.setPIN(slot, password, newPassword);

    System.out.println("Changing the partition password back to '" + password + "'");
    lsm.setPIN(slot, newPassword, password);
  }

}
