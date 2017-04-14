package edu.missouri.operations.reportstart;

import java.sql.SQLException;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.PropertyId;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import edu.missouri.operations.reportcenter.data.SecurityGroups;
import edu.missouri.operations.ui.OracleBooleanCheckBox;
import edu.missouri.operations.ui.OracleTimestampField;

@SuppressWarnings("serial")
public class UsersEditorView extends TopBarView {

	@PropertyId("ID")
	private TextField id;

	/*
	 * @PropertyId("ROWSTAMP") private TextField rowsatmp;
	 */

	@PropertyId("USERLOGIN")
	private TextField userlogin;

	@PropertyId("FULLNAME")
	private TextField fullname;

	@PropertyId("SORTNAME")
	private TextField sortname;

	@PropertyId("EMAILID")
	private TextField emailid;

	@PropertyId("PASSWORD")
	private TextField password;

	@PropertyId("ISACTIVE")
	private OracleBooleanCheckBox IsActive;

	@PropertyId("SECRETKEY")
	private TextField secretkey;

	@PropertyId("CREATED")
	private OracleTimestampField Created;

	@PropertyId("CREATEDAY")
	private TextField createday;
	
	private FieldGroup binder;
	private SQLContainer container; 
    private Item item;
	/*
	 * @PropertyId("MODIFIED") private OracleTimestampField Modified;
	 */

	@Override
	public void attach() {
		super.attach();

		id = new TextField();
		{
			{

			}
		}

		userlogin = new TextField();
		{
			{

			}
		}

		fullname = new TextField();
		{
			{

			}
		}

		sortname = new TextField();
		{
			{

			}
		}

		emailid = new TextField();
		{
			{

			}
		}

		password = new TextField();
		{
			{

			}
		}

		IsActive = new OracleBooleanCheckBox();
		{
			{

				setCaption("Active?");

			}
		}

		secretkey = new TextField();
		{
			{

			}
		}

		Created = new OracleTimestampField();
		{
			{

				setCaption("Date Created");
			}
		}
		createday = new TextField();
		{
			{

			}
		}
		

		// Modified= new OracleTimestampField();{{

		// setCaption("Date Modified");
		// }}

		// Create fields
		// Layout fields

	}

	VerticalLayout layout = new VerticalLayout() {
		{
			setSizeFull();
			addComponent(new HorizontalLayout() {
				{
					addComponent(id);
					addComponent(userlogin);
				}
			});
			addComponent(new HorizontalLayout() {
				{
					addComponent(fullname);
					addComponent(sortname);
				}
			});
			addComponent(new HorizontalLayout() {
				{

					addComponent(emailid);
					addComponent(password);
					addComponent(IsActive);

				}
			});
			addComponent(new HorizontalLayout() {
				{
					addComponent(secretkey);
					addComponent(Created);
					addComponent(createday);
				}
			});
		}
	};

	@Override
	public void enter(ViewChangeEvent event) {
		// TODO Auto-generated method stub
		
		// parameters should be securitygroup id value.
				String parameters = event.getParameters();

				Users query = new Users();
				
				query.setMandatoryFilters(new Compare.Equal("ID",parameters));
				try {
					
					container = new SQLContainer(query);
					item = container.getItem(container.getIdByIndex(1));
					
					binder = new FieldGroup();
					binder.bindMemberFields(item);
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	}

}
