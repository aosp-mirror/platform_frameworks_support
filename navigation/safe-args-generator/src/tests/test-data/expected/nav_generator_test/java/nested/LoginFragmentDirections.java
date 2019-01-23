package foo.flavor.account;

import android.support.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import foo.LoginDirections;

public class LoginFragmentDirections {
    private LoginFragmentDirections() {
    }

    @NonNull
    public static NavDirections register() {
        return new ActionOnlyNavDirections(foo.R.id.register);
    }

    @NonNull
    public static NavDirections actionDone() {
        return LoginDirections.actionDone();
    }
}