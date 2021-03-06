package rocks.inspectit.ui.rcp.ci.wizard.page;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard page for defining name and description. Can be used in multiple wizards. Already existing
 * and forbidden names can be defined.
 *
 * @author Ivan Senic, Alexander Wert
 *
 */
public class DefineNameAndDescriptionWizardPage extends WizardPage {

	/**
	 * Default message.
	 */
	private final String defaultMessage;

	/**
	 * Name box.
	 */
	private Text nameBox;

	/**
	 * Description box.
	 */
	private Text descriptionBox;

	/**
	 * The initial value for the name.
	 */
	private String initialName;

	/**
	 * The initial value for the description.
	 */
	private String initialDescription;

	/**
	 * Main Composite.
	 */
	protected Composite main;

	/**
	 * Existing names.
	 */
	private Collection<String> existingNames;

	/**
	 * Default constructor.
	 *
	 * @param title
	 *            Title of the page.
	 * @param defaultMessage
	 *            Default message for the page.
	 */
	public DefineNameAndDescriptionWizardPage(String title, String defaultMessage) {
		super(title);
		setTitle(title);
		setMessage(defaultMessage);
		this.defaultMessage = defaultMessage;
	}

	/**
	 * Default constructor.
	 *
	 * @param title
	 *            Title of the page.
	 * @param defaultMessage
	 *            Default message for the page.
	 * @param existingNames
	 *            existing names which are not allowed to use
	 */
	public DefineNameAndDescriptionWizardPage(String title, String defaultMessage, Collection<String> existingNames) {
		super(title);
		setTitle(title);
		setMessage(defaultMessage);
		this.defaultMessage = defaultMessage;
		this.existingNames = existingNames;
	}

	/**
	 * Default constructor.
	 *
	 * @param title
	 *            Title of the page.
	 * @param defaultMessage
	 *            Default message for the page.
	 * @param initialName
	 *            initial name
	 * @param initialDescription
	 *            description
	 * @param existingNames
	 *            existing names which are not allowed to use
	 */
	public DefineNameAndDescriptionWizardPage(String title, String defaultMessage, String initialName, String initialDescription, Collection<String> existingNames) {
		super(title);
		this.initialName = initialName;
		this.initialDescription = initialDescription;
		setTitle(title);
		setMessage(defaultMessage);
		this.defaultMessage = defaultMessage;
		this.existingNames = existingNames;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createControl(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));

		Label nameLabel = new Label(main, SWT.LEFT);
		nameLabel.setText("Name:");
		nameBox = new Text(main, SWT.BORDER);
		nameBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (null != initialName) {
			nameBox.setText(initialName);
		}

		Label descLabel = new Label(main, SWT.LEFT);
		descLabel.setText("Description:");
		descLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		descriptionBox = new Text(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		descriptionBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (null != initialDescription) {
			descriptionBox.setText(initialDescription);
		}

		Listener pageCompletionListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				setPageComplete(isPageComplete());
				setPageMessage();
			}
		};
		nameBox.addListener(SWT.Modify, pageCompletionListener);

		setControl(main);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPageComplete() {
		if (nameBox.getText().isEmpty()) {
			return false;
		} else if (CollectionUtils.isNotEmpty(existingNames)) {
			if (existingNames.contains(nameBox.getText().trim())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sets the message based on the page selections.
	 */
	protected void setPageMessage() {
		if (nameBox.getText().isEmpty()) {
			setMessage("No value for the name entered", ERROR);
			return;
		} else if (CollectionUtils.isNotEmpty(existingNames)) {
			if (existingNames.contains(nameBox.getText().trim())) {
				setMessage("This name already exists!", ERROR);
				return;
			}
		}

		setMessage(defaultMessage);
	}

	/**
	 * @return Returns defined name.
	 */
	@Override
	public String getName() {
		return nameBox.getText();
	}

	/**
	 * @return Returns defined description.
	 */
	@Override
	public String getDescription() {
		return descriptionBox.getText();
	}

}
