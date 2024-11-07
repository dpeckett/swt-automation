package com.example;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DemoApp {
  public static void main(String[] args) {
    Display display = new Display();
    Shell shell = new Shell(display);
    shell.setText("Demo");
    shell.setSize(300, 200);

    Label label = new Label(shell, SWT.NONE);
    label.setText("Not clicked");
    label.setBounds(10, 10, 120, 20);

    Button button = new Button(shell, SWT.PUSH);
    button.setText("Button");
    button.setBounds(10, 40, 80, 30);

    button.addListener(SWT.Selection, event -> label.setText("Clicked"));

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) display.sleep();
    }
    display.dispose();
  }
}
