package co.loystar.loystarbusiness.activities;

import android.accounts.AccountManager;
import android.graphics.Typeface;
import android.view.View;

import com.luseen.verticalintrolibrary.VerticalIntro;
import com.luseen.verticalintrolibrary.VerticalIntroItem;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.sync.AccountGeneral;

/**
 * Created by laudbruce-tagoe on 5/22/17.
 */

public class AppIntro extends VerticalIntro {
    @Override
    protected void init() {
        addIntroItem(new VerticalIntroItem.Builder()
                .backgroundColor(R.color.colorPrimaryDark)
                .image(R.drawable.introscreen_1)
                .title(getString(R.string.intro1_title))
                .text(getString(R.string.intro1_body))
                .build());

        addIntroItem(new VerticalIntroItem.Builder()
                .backgroundColor(R.color.colorPrimary)
                .image(R.drawable.introscreen_2)
                .title(getString(R.string.intro2_title))
                .text(getString(R.string.intro2_body))
                .build());

        addIntroItem(new VerticalIntroItem.Builder()
                .backgroundColor(R.color.colorPrimaryDark)
                .image(R.drawable.introscreen_3)
                .title(getString(R.string.intro3_title))
                .text(getString(R.string.intro3_body))
                .build());

        addIntroItem(new VerticalIntroItem.Builder()
                .backgroundColor(R.color.colorPrimary)
                .image(R.drawable.introscreen_4)
                .title(getString(R.string.intro4_title))
                .text(getString(R.string.intro4_body))
                .build());

        addIntroItem(new VerticalIntroItem.Builder()
                .backgroundColor(R.color.colorPrimaryDark)
                .image(R.drawable.introscreen_5)
                .title(getString(R.string.intro5_title))
                .text(getString(R.string.intro5_body))
                .build());


        setSkipEnabled(true);
        setVibrateEnabled(true);
        setVibrateIntensity(20);
        setNextText("CONTINUE");
        setDoneText("GET STARTED");
        setSkipText("Skip");
        setCustomTypeFace(Typeface.createFromAsset(getAssets(), "fonts/Lato.ttf"));
    }

    @Override
    protected Integer setLastItemBottomViewColor() {
        return null;
    }

    @Override
    protected void onSkipPressed(View view) {

    }

    @Override
    protected void onFragmentChanged(int position) {

    }

    @Override
    protected void onDonePressed() {
        AccountManager.get(this).addAccount(AccountGeneral.ACCOUNT_TYPE, null, null, null, AppIntro.this,
                null, null);
    }
}
