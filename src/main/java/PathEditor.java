import com.sun.jna.platform.win32.WinReg;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import static java.util.prefs.Preferences.systemRoot;

import java.util.regex.Pattern;

public class PathEditor {

  private final static String ENVIRONMENT_KEY = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";

  public static void main(String[] args) {
    if (!isRunningAsAdministrator()) {
      System.out.println("PathEditor must be run with administrator privileges.");
      return;
    }

    boolean hasValidArguments = args.length == 2 && (args[0].equals("add") || args[0].equals("remove"));
    if (!hasValidArguments) {
      showHowToUse();
      return;
    }

    try {
      switch (args[0]) {
        case "add" -> add(args[1]);
        case "remove" -> remove(args[1]);
      }
    }
    catch(Exception e) {
       System.exit(1); // Error
    }
  }

  private static boolean isRunningAsAdministrator() {

      PrintStream oldErr = System.err;
      try {
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        Preferences preferences = systemRoot();
        preferences.put("foo", "bar"); // SecurityException on Windows
        preferences.remove("foo");
        preferences.flush(); // BackingStoreException on Linux
        return true;
      } catch (Exception exception) {
        return false;
      } finally {
        System.setErr(oldErr);
      }
    }

  private static void remove(String value) {
    if(value == null || value.trim().isEmpty())
      return;

    value = value.trim();

    if (RegistryAccess.keyExists(WinReg.HKEY_LOCAL_MACHINE, ENVIRONMENT_KEY)) {
      String pathVariables = RegistryAccess.readString(WinReg.HKEY_LOCAL_MACHINE, ENVIRONMENT_KEY, "Path");
      if (pathVariables == null || pathVariables.isEmpty() || !pathVariables.toLowerCase().contains(value.toLowerCase()))
        return;

      pathVariables = pathVariables.trim();
      pathVariables = pathVariables.replaceFirst("(?i)" + Pattern.quote(value) + ";|" + Pattern.quote(value) + "$", "");

      RegistryAccess.writeString(WinReg.HKEY_LOCAL_MACHINE, ENVIRONMENT_KEY, "Path", pathVariables);
    }
  }

  private static void add(String value) {
    if(value == null || value.trim().isEmpty())
      return;

    value = value.trim();

    if (RegistryAccess.keyExists(WinReg.HKEY_LOCAL_MACHINE, ENVIRONMENT_KEY)) {
      String pathVariables = RegistryAccess.readString(WinReg.HKEY_LOCAL_MACHINE, ENVIRONMENT_KEY, "Path");
      if (pathVariables == null || pathVariables.isEmpty())
        pathVariables = value;
      else {
        if (pathVariables.toLowerCase().contains(value.toLowerCase()))
          return;

        pathVariables = pathVariables.trim();
        while (pathVariables.endsWith(";"))
          pathVariables = pathVariables.substring(0, pathVariables.length() - 1);
        pathVariables += ";" + value;
      }

      RegistryAccess.writeString(WinReg.HKEY_LOCAL_MACHINE, ENVIRONMENT_KEY, "Path", pathVariables);
    }
  }

  private static void showHowToUse() {
    System.out.println("""
    PathEditor allows you to safely add or remove values from the system's PATH variable.

    Call this executable with two arguments:
    1. Action "add" or "remove" to add or remove a value
    2. The value to add or remove

    Example:
    java PathEditor add "C:\\Program Files\\RepoZ"

    Note:
    You should put your value in quotes ("") if it contains spaces.
          This app is case insensitive, it will ignore value casing.
    """);
  }
}
