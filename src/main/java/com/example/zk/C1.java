package com.example.zk;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Window;
import org.zkoss.zul.Window;

//public class C1 extends SelectorComposer <Window>{
public class C1 extends SelectorComposer<Div> {
	private static final long serialVersionUID = 1L;

	public Button btn1;

	public C1() {
		// this.doAfterCompose();
		// btn1 = (Button) this.getSelf(),getf
	}
	//  กรณีเป็น windows
	//	public void doAfterCompose(Window w) {
	//		//super.doAfterCompose(w);
	//		System.out.println("this.getSelf().getId() :" +this.getSelf().getId());
	//		System.out.println("w.getId() :"+w.getId());
	//	}

	public void doAfterCompose(Div w) {
		// super.doAfterCompose(w);
		System.out.println("this.getSelf().getId() :" + this.getSelf().getId());
		System.out.println("w.getId() :" + w.getId());
		btn1 = (Button) this.getSelf().getFellow("btn1"); // test ผ่าน
		btn1.addEventListener(Events.ON_CLICK, event -> onClick_BtnDelRow(event));
	}

	private void onClick_BtnDelRow(Event event) {
		// test ok 18/1/67
		System.out.println("click1()");

		Window win1 = new Window("xx", "NORMAL", true);

		//win1.setParent(this.getSelf().getParent()); หรือ
		win1.setParent(this.getSelf());

		win1.setWidth("1100px");
		win1.setHeight("300px");

		List<Component> ch1 = win1.getChildren();
		ch1.add(new Button("btn1"));
		ch1.add(new Button("btn2"));

		//win1.doPopup();
		win1.doOverlapped();
	}

	@Listen("onUpdateTxt1")
	public void onUpdateTxt1(Event evt) {

		try {

			Object[] data = (Object[]) evt.getData();

			System.out.println("data[0] : " + data[0].toString());//data[0] จะมีค่าเป็น "image/svg+xml;base64";
			System.out.println("data[1] : " + data[1]);

			//txt1.setValue(txt1.getValue() + "\n" + data[0].toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}