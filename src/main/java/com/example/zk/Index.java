package com.example.zk;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Window;

public class Index extends SelectorComposer<Div> {
	
	private static final long serialVersionUID = 1L;

	@Wire
	public Button btnClear;

	public Index() {
	}

	public void doAfterCompose(Div cn) throws Exception {
		super.doAfterCompose(cn); // สำคัญ ต้องมีถ้าใช้ @Wire
		System.out.println("cn.getId() :" + cn.getId());
		System.out.println("btn1.getLabel() :" + btnClear.getLabel());
	}

	@Listen("onClick = #btnClear")
	public void onClick_btn1() {
		// test ok 18/1/67
		System.out.println("onClick_btn1()");

		Window win1 = new Window("xx", "NORMAL", true);

		win1.setParent(this.getSelf());

		win1.setWidth("1100px");
		win1.setHeight("300px");

		List<Component> ch1 = win1.getChildren();
		ch1.add(new Button("btn1"));
		ch1.add(new Button("btn2"));

		//win1.doPopup();
		//win1.doOverlapped();
		win1.doModal();
	}

}