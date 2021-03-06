package com.joshua.a51bike.activity.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

import com.joshua.a51bike.Interface.PaySuccess;
import com.joshua.a51bike.R;
import com.joshua.a51bike.activity.core.BaseActivity;
import com.joshua.a51bike.activity.dialog.WaitProgress;
import com.joshua.a51bike.entity.User;
import com.joshua.a51bike.pay.util.PayUtils;
import com.joshua.a51bike.util.AppUtil;
import com.joshua.a51bike.util.UiUtils;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import static com.joshua.a51bike.pay.util.PayUtils.payUtils;

/**
 * class description here
 *
 * 交押金界面
 *
 * @version 1.0.0
 * @outher wangqiang
 * @project 51Bike
 * @since 2017-03-10
 */
@ContentView(R.layout.account_yajin)
public class AccountYaJin extends BaseActivity {
    private static final String TAG = "AccountYaJin";

    @ViewInject(R.id.pay_zhifubao)
    private RadioButton zhifubao;

    @ViewInject(R.id.pay_weixin)
    private RadioButton weixin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    public void init() {
        initActionBar();
        findId();
        setLister();
    }

    private void initActionBar() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle("");
        myToolbar.setNavigationIcon(getResources().getDrawable(R.drawable.title_back));
        setSupportActionBar(myToolbar);
    }

    public void findId() {

    }

    public void setLister() {
        findViewById(R.id.recharge).setOnClickListener(this);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left_back:
                finish();
                break;
            case R.id.recharge:
                reChange();
                break;
            default:
                break;
        }
    }

    private void reChange() {
        if(zhifubao.isChecked()){
            UiUtils.showToast("正在跳转");
            PayUtils patils = PayUtils.getPayUtils();
            payUtils.setPaySuccess(new myPaySuccess());
            payUtils.payV2(this,200);
        }
        else {
            post();
//            uiUtils.showToast("微信支付暂时未开通");

        }


    }
    private class myPaySuccess implements PaySuccess {
        @Override
        public void onSccuess() {
            post();
        }
    }

    private void success() {
        UiUtils.showToast("支付成功");
        User user = userControl.getUser();
        user.setUserRerve(200);
        userControl.setUser(user);
        finish();
    }

    private String url = AppUtil.BaseUrl+"/user/insertRerver";
    private void post(){
        dialogControl.setDialog(new WaitProgress(this));
        dialogControl.show();
        RequestParams result_params = new RequestParams(url);
        result_params.addParameter("userid",userControl.getUser().getUserid());
        Log.i(TAG, "post: param  "+result_params.toString() );
        x.http().get(result_params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.i(TAG, "onSuccess: result "+result);
                if(result.equals("ok")){
                    dialogControl.cancel();
                    success();
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                ex.printStackTrace();
                Log.i("msp", ">>>>>>>>>>>>>>>>>>>>>>>>>>o2:"+ex.getMessage());
                dialogControl.cancel();
            }

            @Override
            public void onCancelled(CancelledException cex) {
                dialogControl.cancel();

            }

            @Override
            public void onFinished() {

            }
        });

    }
        @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when an activity you launched exits
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
  
  
  
  
