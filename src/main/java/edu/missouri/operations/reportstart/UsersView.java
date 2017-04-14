package edu.missouri.operations.reportstart;

import java.sql.SQLException;

import com.vaadin.data.util.sqlcontainer.OracleContainer;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import edu.missouri.operations.reportcenter.data.Users;
import edu.missouri.operations.reportcenter.ui.TopBarView;
import edu.missouri.operations.ui.StandardTable;
import edu.missouri.operations.ui.TableColumn;
import edu.missouri.operations.ui.desktop.buttons.AddButton;
import edu.missouri.operations.ui.desktop.buttons.DeleteButton;
import edu.missouri.operations.ui.desktop.buttons.EditButton;

public class UsersView extends TopBarView {

	private StandardTable table;
	private AddButton addButton;
	private EditButton editButton;
	private DeleteButton deleteButton;

	public UsersView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void attach() {
		super.attach();
		init();
	}

	@SuppressWarnings("serial")
	private void init() {

		table = new StandardTable() {
			{
				add(new TableColumn("USERLOGIN", "Login"));
				add(new TableColumn("FULLNAME", "Full Name"));
				add(new TableColumn("SORTNAME", "Sort Name"));
				add(new TableColumn("ISACTIVE", "Active?"));
				add(new TableColumn("CREATED", "Created"));
				add(new TableColumn("CREATEDBY", "Created By"));
			}
		};

		addButton = new AddButton() {
			{
			}
		};
		editButton = new EditButton() {
			{
			}
		};

		deleteButton = new DeleteButton() {
			{
			}
		};

		addInnerComponent(new VerticalLayout() {
			{
				setSizeFull();
				setMargin(true);
				addComponent(new Label("Users", ContentMode.HTML) {
					{
						addStyleName("maintitle");
					}
				});
				addComponent(new HorizontalLayout() {
					{
						addComponent(addButton);
						addComponent(editButton);
						addComponent(deleteButton);
					}
				});
				addComponent(table);
				setExpandRatio(table, 1.0f);

			}
		});

	}

	@Override
	public void enter(ViewChangeEvent event) {
		
		try {
			
			Users query = new Users();
			OracleContainer container = new OracleContainer(query);
			table.setContainerDataSource(container);
			table.configure();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
