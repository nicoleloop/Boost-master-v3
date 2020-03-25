package info.nightscout.androidaps.plugins.pump.omnipod.dialogs.wizard.pages;

import com.atech.android.library.wizardpager.defs.action.AbstractCancelAction;
import com.atech.android.library.wizardpager.defs.action.FinishActionInterface;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.CareportalEvent;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload;
import info.nightscout.androidaps.plugins.pump.omnipod.defs.SetupProgress;
import info.nightscout.androidaps.plugins.pump.omnipod.dialogs.PodManagementActivity;
import info.nightscout.androidaps.plugins.pump.omnipod.dialogs.wizard.defs.PodActionType;
import info.nightscout.androidaps.plugins.pump.omnipod.driver.OmnipodDriverState;
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.SP;

/**
 * Created by andy on 12/11/2019
 */
public class InitPodRefreshAction extends AbstractCancelAction implements FinishActionInterface {

    private static final Logger LOG = LoggerFactory.getLogger(L.PUMP);
    private PodManagementActivity podManagementActivity;
    private PodActionType actionType;

    public InitPodRefreshAction(PodManagementActivity podManagementActivity, PodActionType actionType) {
        this.podManagementActivity = podManagementActivity;
        this.actionType = actionType;
    }

    @Override
    public void execute(String cancelReason) {
        if (cancelReason != null && cancelReason.trim().length() > 0) {
            this.cancelActionText = cancelReason;
        }

        if (this.cancelActionText.equals("Cancel")) {
            //AapsOmnipodManager.getInstance().resetPodStatus();
        }

        podManagementActivity.refreshButtons();
    }

    @Override
    public void execute() {
        if (actionType==PodActionType.InitPod) {
            if (OmnipodUtil.getPodSessionState().getSetupProgress().isBefore(SetupProgress.COMPLETED)) {
                OmnipodUtil.setDriverState(OmnipodDriverState.Initalized_PodInitializing);
            } else {
                OmnipodUtil.setDriverState(OmnipodDriverState.Initalized_PodAttached);
                uploadCareportalEvent(System.currentTimeMillis(), CareportalEvent.SITECHANGE);
            }
        } else {
            OmnipodUtil.setDriverState(OmnipodDriverState.Initalized_NoPod);
        }

        podManagementActivity.refreshButtons();
    }

    private void uploadCareportalEvent(long date, String event) {
        if (MainApp.getDbHelper().getCareportalEventFromTimestamp(date) != null)
            return;
        try {
            JSONObject data = new JSONObject();
            String enteredBy = SP.getString("careportal_enteredby", "");
            if (!enteredBy.equals("")) data.put("enteredBy", enteredBy);
            data.put("created_at", DateUtil.toISOString(date));
            data.put("eventType", event);
            CareportalEvent careportalEvent = new CareportalEvent();
            careportalEvent.date = date;
            careportalEvent.source = Source.USER;
            careportalEvent.eventType = event;
            careportalEvent.json = data.toString();
            MainApp.getDbHelper().createOrUpdate(careportalEvent);
            NSUpload.uploadCareportalEntryToNS(data);
        } catch (JSONException e) {
            LOG.error("Unhandled exception when uploading SiteChange event.", e);
        }
    }


    @Override
    public String getFinishActionText() {
        return "Finish_OK";
    }
}
