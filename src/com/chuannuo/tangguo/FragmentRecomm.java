/* 
 * @Title:  FragmentRecomm.java 
 * @Copyright:  XXX Co., Ltd. Copyright YYYY-YYYY,  All rights reserved 
 * @Description:  TODO<请描述此文件是做什么的> 
 * @author:  xie.xin
 * @data:  2015-7-18 上午1:45:47 
 * @version:  V1.0 
 */
package com.chuannuo.tangguo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chuannuo.tangguo.listener.AddPointListener;
import com.chuannuo.tangguo.listener.GetTotalPointListener;
import com.chuannuo.tangguo.listener.ResponseStateListener;
import com.chuannuo.tangguo.listener.SpendPointListener;
import com.chuannuo.tangguo.task.SpendPointTask;

import android.R.integer;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

/**
 * TODO<请描述这个类是干什么的>
 * 
 * @author xie.xin
 * @data: 2015-7-18 上午1:45:47
 * @version: V1.0
 */
public class FragmentRecomm extends BaseFragment {

	private ListView myListView;
	private LinearLayout view;

	private ArrayList<AppInfo> recommList;
	private RecommendTaskAdapter adapter;
	// 获取手机内所有应用
	private List<PackageInfo> paklist;
	private double score = 1;
	private int isShow = 0;
	private String textName = "积分";
	private boolean isFirst = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		isFirst = true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		initView();
		if(isFirst){
			initProgressDialog(Constant.StringValues.LOADING);
			if (null == recommList) {
				recommList = new ArrayList<AppInfo>();
			}
			paklist = getActivity().getPackageManager().getInstalledPackages(0);
			/*
			 * 上报手机已经安装的软件 各个应用包名用，隔开eg:
			 * cn.winads.studentsearn,cn.winads.ldbatterySteward
			 */
			reportInstalledApp();
			isFirst = false;
		}else{
//			if (null == adapter) {
//				adapter = new RecommendTaskAdapter(getActivity(),
//						recommList, myListView);
//			} else {
//				adapter.notifyDataSetChanged();
//			}
			myListView.setAdapter(adapter);
		}
		return view;
	}
	
	@Override
	public void onResume() {
		if (recommList != null && recommList.size() > 0) {
			AppInfo app = new AppInfo();
			for (int i = recommList.size() - 1; i >= 0; i--) {
				app = recommList.get(i);
				if (app.getResource_id() == pref
						.getInt(Constant.RESOURCE_ID, 0)) {
					recommList.remove(i);
					break;
				}
			}
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}

		}
		super.onResume();
	}

	private void initView() {
		view = super.getRootLinearLayout();

		myListView = new ListView(getActivity());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		myListView.setDivider(new ColorDrawable(Color
				.parseColor(Constant.ColorValues.DIVIDER_COLOR)));
		myListView.setDividerHeight(1);
		myListView.setId(Constant.IDValues.LV_RECOMM);
		myListView.setLayoutParams(lp);

		myListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mListener.onBtnClickListener(Constant.STEP_1,
						recommList.get(position));
			}
		});
		view.addView(myListView);
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 2:
				initData();
				break;
			case 3:
				if (null == adapter) {
					adapter = new RecommendTaskAdapter(getActivity(),
							recommList, myListView);
				} else {
					adapter.notifyDataSetChanged();
				}
				myListView.setAdapter(adapter);
				break;
			default:
				break;
			}
		}

	};

	/**
	 * @Title: reportInstalledApp
	 * @Description: 上报应用
	 * @param
	 * @return void
	 * @throws
	 */
	private void reportInstalledApp() {
		StringBuilder pakStr = new StringBuilder();
		for (int i = 0; i < paklist.size(); i++) {
			PackageInfo pak = (PackageInfo) paklist.get(i);
			// 判断是否为非系统预装的应用程序
			if ((pak.applicationInfo.flags & pak.applicationInfo.FLAG_SYSTEM) <= 0) {
				pakStr.append(pak.applicationInfo.packageName);
				pakStr.append(",");
			}
		}
		if (pakStr.length() > 0) {
			pakStr.deleteCharAt(pakStr.length() - 1);
		}
		progressDialog.show();
		HttpUtil.setParams("app_id", pref.getString(Constant.APP_ID, "0"));
		HttpUtil.setParams("package_names", pakStr.toString());
		HttpUtil.post(Constant.URL.REPORT_URL, new ResponseStateListener() {

			@Override
			public void onSuccess(Object result) {
				if (null != result && !result.equals(Constant.NET_ERROR)) {
					JSONObject jsonObject;
					try {
						jsonObject = new JSONObject(result.toString());
						String code = jsonObject.getString("code");
						if (code.equals("1")) {
							Message msg = mHandler.obtainMessage();
							msg.what = 2;
							mHandler.sendMessage(msg);
						} else {
							Toast.makeText(getActivity(), "获取数据失败",
									Toast.LENGTH_SHORT).show();
							progressDialog.dismiss();
						}
					} catch (Exception e) {
						Toast.makeText(getActivity(), "获取数据失败！",
								Toast.LENGTH_SHORT).show();
						progressDialog.dismiss();
					}
				} else {
					Toast.makeText(getActivity(), "获取数据失败！", Toast.LENGTH_SHORT)
							.show();
					progressDialog.dismiss();
				}
			}
		});
	}

	private void initData() {

		if (recommList.size() <= 0) {
			HttpUtil.setParams("app_id", pref.getString(Constant.APP_ID, "0"));
			HttpUtil.setParams("channel_id", TangGuoWall.APPID);
			HttpUtil.post(Constant.URL.RESOURCE_URL,
					new ResponseStateListener() {

						@Override
						public void onSuccess(Object result) {
							if (null != result
									&& !result.equals(Constant.NET_ERROR)) {
								JSONObject jsonObject;
								JSONObject infoObject;
								try {
									jsonObject = new JSONObject(result
											.toString());
									String code = jsonObject.getString("code");
									if (code.equals("1")) {
										infoObject = jsonObject
												.getJSONObject("info");
										if (null != infoObject) {
											score = infoObject
													.getDouble("txt_vc_price");
											isShow = infoObject
													.getInt("is_show_integral");
											textName = infoObject
													.getString("txt_name");
										}
										JSONArray jArray = jsonObject
												.getJSONArray("data");

										if (null != jArray
												&& jArray.length() > 0) {
											for (int i = 0; i < jArray.length(); i++) {
												JSONObject obj = jArray
														.getJSONObject(i);
												if (null != obj) {
													AppInfo appInfo = new AppInfo();
													appInfo.setIsShow(isShow);
													appInfo.setTextName(textName);

													appInfo.setVcPrice(score);
													appInfo.setResource_id(obj
															.getInt("id"));
													appInfo.setAdId(obj
															.getInt("ad_id"));
													appInfo.setTitle(obj
															.getString("title"));
													appInfo.setName(obj
															.getString("name"));
													if(appInfo.getTitle().equals("")){
														appInfo.setTitle(appInfo.getName());
													}
													appInfo.setDescription(obj
															.getString("description"));
													appInfo.setPackage_name(obj
															.getString("package_name"));
													appInfo.setBrief(obj
															.getString("brief"));
													appInfo.setScore((int) (obj
															.getInt("score") * score));
													appInfo.setResource_size(obj
															.getString("resource_size"));
													appInfo.setB_type(obj
															.getInt("btype"));
													appInfo.setTotalScore((int) ((obj
															.getInt("score") + obj
															.getInt("sign_number") * 10) * score));
													appInfo.setSign_rules(obj
															.getInt("reportsigntime"));
													appInfo.setNeedSign_times(obj
															.getInt("sign_number"));
													String fileUrl = obj
															.getString("file");
													String iconUrl = obj
															.getString("icon");
													String h5Url = obj
															.getString("h5_big_url");
													appInfo.setAlert(obj
															.getString("alert") == null ? ""
															: obj.getString("alert"));

													if (!fileUrl
															.contains("http")) {
														fileUrl = Constant.URL.ROOT_URL
																+ fileUrl;
													}
													if (!iconUrl
															.contains("http")) {
														iconUrl = Constant.URL.ROOT_URL
																+ iconUrl;
													}
													if (!h5Url.contains("http")) {
														h5Url = Constant.URL.ROOT_URL
																+ h5Url;
													}

													appInfo.setFile(fileUrl);
//													String dUrl = Constant.PREF_TANGGUO_DATA+appInfo.getAdId();
//													editor.putString(dUrl, fileUrl);
//													editor.commit();
													appInfo.setH5_big_url(h5Url);
													appInfo.setIcon(iconUrl);

													recommList.add(appInfo);
												}
											}
										}

										Message msg = mHandler.obtainMessage();
										msg.what = 3;
										mHandler.sendMessage(msg);
									} else {
										Toast.makeText(getActivity(), "获取数据失败",
												Toast.LENGTH_SHORT).show();
										progressDialog.dismiss();
									}
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} finally {
									progressDialog.dismiss();
								}

							} else {
								Toast.makeText(getActivity(), "获取数据失败！",
										Toast.LENGTH_SHORT).show();
								progressDialog.dismiss();
							}
						}

					});
		} else {
			if (progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			if (null == adapter) {
				adapter = new RecommendTaskAdapter(getActivity(), recommList,
						myListView);
			} else {
				adapter.notifyDataSetChanged();
			}
			myListView.setAdapter(adapter);
		}
	}
}
