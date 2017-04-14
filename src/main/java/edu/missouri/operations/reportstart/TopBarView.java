package edu.missouri.operations.reportstart;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c10n.C10N;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import edu.missouri.operations.data.User;
import edu.missouri.operations.reportcenter.data.SecurityGroupUsers;
import edu.missouri.operations.reportcenter.ui.c10n.TopBarText;

/**
 * @author graumannc
 * 
 */
@SuppressWarnings("serial")
public abstract class TopBarView extends VerticalLayout implements View {

	protected TopBarView view;

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	protected Button homeButton;
	protected Button configurationButton;
	private Button logoffButton;

	protected TopBarText topBarText;

	protected VerticalLayout notificationLayout;

	private Label copyright;

	public void trace(String message) {

		if (logger.isDebugEnabled()) {
			logger.debug("{} - {}", message, new java.util.Date().getTime());
		}

	}

	public TopBarView() {
		super();
		init();
		layout();
	}

	public TopBarView(Component... children) {
		super(children);
	}

	@Override
	public void attach() {

		super.attach();
		view = this;

		if (User.getUser() != null && !SecurityGroupUsers.memberOf("ADMINISTRATORS", User.getUser().getUserId())) {
			configurationButton.setVisible(false);
			configurationButton.setEnabled(false);
		}

		// TODO Remove after security is set up.
		configurationButton.setVisible(true);
		configurationButton.setEnabled(true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener
	 * .ViewChangeEvent)
	 */
	@Override
	public abstract void enter(ViewChangeEvent event);

	private void init() {

		topBarText = C10N.get(TopBarText.class, Locale.ENGLISH);

		homeButton = new TopbarButton(ReportCenterViewProvider.Views.HOME, "Operations ReportCenter") {
			{
				addStyleName("topbarbutton borderless");
				setIcon(new ThemeResource("images/mulogotb.png"));
				setDescription(topBarText.projectsHome());
			}
		};

		configurationButton = new TopbarButton(ReportCenterViewProvider.Views.CONFIGURATION, topBarText.configuration()) {
			{
				addStyleName("topbarbutton borderless");
				setIcon(new ThemeResource("icons/chalkwork/basic/settings_16x16.png"));
				setDescription(topBarText.configuration_help());
			}
		};

		logoffButton = new NativeButton(topBarText.signOff()) {
			{
				addStyleName("topbarbutton borderless");
				setIcon(new ThemeResource("icons/general/small/Sign_Out.png"));
				setDescription(topBarText.signOff_help());

				addClickListener(new Button.ClickListener() {
					@Override
					public void buttonClick(ClickEvent event) {

						if (logger.isDebugEnabled()) {
							
							logger.debug("Sign off clicked");
						}

						addStyleName("selected");
						User.setUser(null);
						UI.getCurrent().getPage().setLocation("");
						UI.getCurrent().getSession().close();
					}
				});
			}
		};

		copyright = new Label("&copy; 2017 鈥� Curators of the University of Missouri. All rights reserved. An equal opportunity/access/affirmative action/pro-disabled and veteran employer.",
				ContentMode.HTML) {
			{
				addStyleName("copyright");
			}
		};

	}

	Component innerComponent;

	private void layout() {

		setSizeFull();
		// addStyleName("bordered mainscreen");
		setMargin(false);

		// start of button bar
		addComponent(new HorizontalLayout() {
			{
				addStyleName("topbar");
				setWidth("100%");

				CssLayout menu = new CssLayout() {
					{
						addStyleName("menu");
						setWidth("100%");
						addComponent(homeButton);

					}
				};
				addComponent(menu);
				setExpandRatio(menu, 1.0f);

				CssLayout menu2 = new CssLayout() {
					{
						addStyleName("menu");
						addComponent(configurationButton);
						addComponent(logoffButton);
					}
				};

				addComponent(menu2);
				setComponentAlignment(menu2, Alignment.TOP_RIGHT);
			}
		});

		innerComponent = new Label("");

		addComponent(innerComponent);
		setExpandRatio(innerComponent, 1.0f);

		addComponent(new HorizontalLayout() {
			{
				addStyleName("bottombar");
				setWidth("100%");
				addComponent(copyright);
			}
		});

	}

	public void addInnerComponent(Component c) {
		replaceComponent(innerComponent, c);
		setExpandRatio(c, 1.0f);
		innerComponent = c;
	}

	public Component addNotification(String message) {

		final HorizontalLayout notification = new HorizontalLayout() {
			{
				setSpacing(true);
				setMargin(true);
				setStyleName("onscreennotification");
				setWidth("100%");

			}
		};

		Button closeButton = new Button() {
			{
				setIcon(new ThemeResource("icons/special/notification_close.png"));
				setDescription("Close");
				setStyleName("borderless");

				addClickListener(new ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						notification.setVisible(false);
					}

				});
			}
		};

		notification.addComponent(closeButton);

		Label messageLabel = new Label(message) {
			{
				setWidth("100%");
			}
		};
		notification.addComponent(messageLabel);
		notification.setExpandRatio(messageLabel, 1.0f);

		notificationLayout.addComponent(notification);

		return notification;

	}

	public void removeNotification(Component notification) {
		notificationLayout.removeComponent(notification);
	}

	public void removeAllNotifications() {
		notificationLayout.removeAllComponents();
	}

	public void resetScreen() {

		String fragment = getUI().getPage().getUriFragment();
		String parameter = fragment.substring(fragment.indexOf("/") + 1);
		setScreenData(parameter);

	}

	public void setConfigurationEnabled(boolean enabled) {
		configurationButton.setEnabled(enabled);
		configurationButton.setVisible(enabled);
	}

	public void saveScreen() {

	}

	public void setScreenData(String o) {

	}

}
