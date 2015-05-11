// Generated code from Butter Knife. Do not modify!
package com.ndipatri.roboButton.fragments;

import android.view.View;
import butterknife.Views.Finder;

public class ButtonDetailsDialogFragment$$ViewInjector {
  public static void inject(Finder finder, com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment target, Object source) {
    View view;
    view = finder.findById(source, 2131230791);
    target.nameEditText = (android.widget.EditText) view;
    view = finder.findById(source, 2131230792);
    target.autoModeSwitch = (android.widget.Switch) view;
  }

  public static void reset(com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment target) {
    target.nameEditText = null;
    target.autoModeSwitch = null;
  }
}
