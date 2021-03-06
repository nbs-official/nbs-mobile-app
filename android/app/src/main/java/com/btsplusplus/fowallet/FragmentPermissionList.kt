package com.btsplusplus.fowallet

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import bitshares.*
import com.fowallet.walletcore.bts.BitsharesClientManager
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FragmentPermissionList.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FragmentPermissionList.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FragmentPermissionList : BtsppFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var _ctx: Context? = null
    private var _view: View? = null
    private var _data: JSONArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun _onQueryDependencyAccountNameResponsed() {
        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        _data!!.forEach<JSONObject> {
            val row = it!!
            row.getJSONArray("items").forEach<JSONObject> {
                val authority_item = it!!
                if (authority_item.optBoolean("isaccount")) {
                    val oid = authority_item.getString("key")
                    val account = chainMgr.getChainObjectByID(oid)
                    authority_item.put("name", account.getString("name"))
                }
            }
        }

        //  ??????
        _refreshUI()
    }

    /*
     *  (public) ??????????????????TAB - ??????????????????????????????????????????????????????????????????????????????APP?????????????????????
     */
    fun refreshCurrAccountData() {
        activity?.let { ctx ->
            val curr_full_account_data = WalletManager.sharedWalletManager().getWalletAccountInfo()!!
            val curr_account_id = curr_full_account_data.getJSONObject("account").getString("id")

            val chainMgr = ChainObjectManager.sharedChainObjectManager()

            //  ???????????????????????? & ??????????????????????????????
            val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(ctx), ctx)
            mask.show()

            chainMgr.queryFullAccountInfo(curr_account_id).then {
                val full_data = it as JSONObject
                //  [?????????] ??????????????????????????????
                AppCacheManager.sharedAppCacheManager().updateWalletAccountInfo(full_data)
                //  ??????????????????????????? data_array ???
                _initDataArrayWithFullAccountData(full_data)
                //  ????????????
                val account_id_hash = JSONObject()
                _data!!.forEach<JSONObject> {
                    val row = it!!
                    row.getJSONArray("items").forEach<JSONObject> {
                        val authority_item = it!!
                        if (authority_item.optBoolean("isaccount")) {
                            account_id_hash.put(authority_item.getString("key"), true)
                        }
                    }
                }
                if (account_id_hash.length() <= 0) {
                    //  ??????????????????????????????????????????
                    mask.dismiss()
                    //  ??????
                    _refreshUI()
                    return@then null
                } else {
                    //  ????????????
                    return@then chainMgr.queryAllAccountsInfo(account_id_hash.keys().toJSONArray()).then {
                        mask.dismiss()
                        _onQueryDependencyAccountNameResponsed()
                        return@then null
                    }
                }
            }.catch {
                mask.dismiss()
                showToast(resources.getString(R.string.tip_network_error))
            }
            return@let
        }
    }

    /*
     *  (private) ????????????
     *  full_account_data - ??????????????????????????????????????????????????????????????????????????????
     */
    private fun _refreshUI(new_full_account_data: JSONObject? = null) {
        if (new_full_account_data != null) {
            _initDataArrayWithFullAccountData(new_full_account_data)
        }
        drawUI()
    }

    /**
     * (private) ????????????
     */
    private fun drawUI() {
        //  clear all
        val parent_layout = _view!!.findViewById<LinearLayout>(R.id.layout_of_fragment_permission_list)
        parent_layout.removeAllViews()

        //  draw all
        var index = 1
        _data!!.forEach<JSONObject> {
            val item = it!!
            val is_memo = item.optBoolean("is_memo")
            val passThreshold = item.getInt("weight_threshold")

            //  ???????????? , ??????
            val layout_title_info = LinearLayout(_ctx).apply {
                layoutParams = LinearLayout.LayoutParams(LLAYOUT_MATCH, LLAYOUT_WARP)
                orientation = LinearLayout.HORIZONTAL

                val layout_left = LinearLayout(_ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL

                    layoutParams = LinearLayout.LayoutParams(0, LLAYOUT_WARP, 1.0f).apply {
                        gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    }

                    //  ????????????
                    val tv_account_name = TextView(_ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(LLAYOUT_WARP, LLAYOUT_WARP).apply {
                            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                            setMargins(0, 0, 5.dp, 0)
                        }
                        gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.0f)
                        setTextColor(resources.getColor(R.color.theme01_textColorHighlight))
                        text = "$index. ${item.getString("title")}"
                    }

                    //  ????????????????????????
                    val iv_edit = if (item.getBoolean("canBeModified")) {
                        ImageView(_ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(LLAYOUT_WARP, LLAYOUT_WARP).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                            }
                            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                            setImageDrawable(resources.getDrawable(R.drawable.icon_edit))
                            scaleType = ImageView.ScaleType.FIT_END
                            setColorFilter(resources.getColor(R.color.theme01_textColorHighlight))

                            setOnClickListener {
                                onEditPermission(item)
                            }
                        }
                    } else {
                        null
                    }
                    addView(tv_account_name)
                    iv_edit?.let { addView(it) }
                }
                addView(layout_left)

                //  ??????  ????????????????????????
                if (!is_memo) {
                    val layout_right = LinearLayout(_ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

                        layoutParams = LinearLayout.LayoutParams(0, LLAYOUT_WARP, 1.0f).apply {
                            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                        }

                        val tv_threshold_name = TextView(_ctx).apply {
                            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                            text = resources.getString(R.string.kVcPermissionPassThreshold)
                            setTextColor(resources.getColor(R.color.theme01_textColorNormal))
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                        }

                        val tv_threshold_value = TextView(_ctx).apply {
                            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                            text = item.getString("weight_threshold")
                            setPadding(2.dp, 0, 0, 0)
                            setTextColor(resources.getColor(R.color.theme01_textColorMain))
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                        }

                        addView(tv_threshold_name)
                        addView(tv_threshold_value)
                    }
                    addView(layout_right)
                }
            }
            parent_layout.addView(layout_title_info)

            //  ????????????????????????/??????  ??????     ?????????????????????
            if (!is_memo) {
                val layout_account_title_info = LinearLayout(_ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(LLAYOUT_MATCH, LLAYOUT_WARP).apply {
                        setMargins(0, 10.dp, 0, 5.dp)
                    }
                    orientation = LinearLayout.HORIZONTAL

                    val layout_left = LinearLayout(_ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL

                        layoutParams = LinearLayout.LayoutParams(0, LLAYOUT_WARP, 1.0f).apply {
                            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                        }

                        val tv_title_name = TextView(_ctx).apply {
                            text = resources.getString(R.string.kVcPermissionEditTitleName)
                            setTextColor(resources.getColor(R.color.theme01_textColorGray))
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                        }

                        addView(tv_title_name)

                    }

                    val layout_right = LinearLayout(_ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

                        layoutParams = LinearLayout.LayoutParams(0, LLAYOUT_WARP, 1.0f).apply {
                            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                        }

                        val tv_weight_name = TextView(_ctx).apply {
                            text = resources.getString(R.string.kVcPermissionEditTitleWeight)
                            setTextColor(resources.getColor(R.color.theme01_textColorGray))
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                        }
                        addView(tv_weight_name)
                    }
                    addView(layout_left)
                    addView(layout_right)
                }
                parent_layout.addView(layout_account_title_info)

                item.getJSONArray("items").forEach<JSONObject> {
                    val authority_item = it!!

                    //  ??????????????????????????????????????????????????????100%??????
                    val threshold = authority_item.getInt("threshold")
                    var weight_percent = threshold.toDouble() * 100.0 / passThreshold.toDouble()
                    if (threshold < passThreshold) {
                        weight_percent = min(weight_percent, 99.0)
                    }
                    if (threshold > 0) {
                        weight_percent = max(weight_percent, 1.0)
                    }
                    weight_percent = min(weight_percent, 100.0)

                    val layout_permission_weight = LinearLayout(_ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(LLAYOUT_MATCH, 24.dp).apply {
                            setMargins(0, 0.dp, 0, 5.dp)
                        }
                        orientation = LinearLayout.HORIZONTAL


                        val layout_left = LinearLayout(_ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL

                            layoutParams = LinearLayout.LayoutParams(0, LLAYOUT_WARP, 3.0f).apply {
                                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                            }
                            val tv_admin_public_key = TextView(_ctx).apply {
                                text = authority_item.optString("name", null) ?: authority_item.getString("key")
                                setTextColor(resources.getColor(R.color.theme01_textColorMain))
                                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                                setSingleLine(true)
                                maxLines = 1
                                ellipsize = TextUtils.TruncateAt.END
                            }
                            addView(tv_admin_public_key)
                        }

                        val layout_right = LinearLayout(_ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

                            layoutParams = LinearLayout.LayoutParams(0, LLAYOUT_WARP, 1.0f).apply {
                                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                            }

                            val tv_weight = TextView(_ctx).apply {
                                text = authority_item.getString("threshold")
                                setTextColor(resources.getColor(R.color.theme01_textColorMain))
                                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                            }
                            val tv_weight_percent = TextView(_ctx).apply {
                                text = " (${weight_percent.toInt()}%)"
                                setTextColor(resources.getColor(R.color.theme01_textColorMain))
                                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                            }
                            addView(tv_weight)
                            addView(tv_weight_percent)
                        }

                        addView(layout_left)
                        addView(layout_right)
                    }
                    parent_layout.addView(layout_permission_weight)
                }
            } else {
                //  ????????????????????????
                val first_authority_item = item.getJSONArray("items").getJSONObject(0)
                val tv_weight = TextView(_ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(LLAYOUT_MATCH, LLAYOUT_WARP).apply {
                        setMargins(0, 10.dp, 0, 5.dp)
                    }
                    text = first_authority_item.getString("key")
                    setTextColor(resources.getColor(R.color.theme01_textColorMain))
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
                    setSingleLine(true)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                parent_layout.addView(tv_weight)
            }
            parent_layout.addView(ViewLine(_ctx!!, 10.dp, 10.dp))
            index++
        }
    }

    /*
     *  (private) ????????????????????????
     */
    private fun _onModifyMemoKeyClicked(permissionItem: JSONObject, newKey: String) {
        if (!OrgUtils.isValidBitsharesPublicKey(newKey)) {
            showToast(resources.getString(R.string.kVcPermissionSubmitTipsInputValidMemoKey))
            return
        }

        val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), _ctx)
        mask.show()
        val account_data = WalletManager.sharedWalletManager().getWalletAccountInfo()!!.getJSONObject("account")
        ChainObjectManager.sharedChainObjectManager().queryAccountData(account_data.getString("id")).then {
            mask.dismiss()
            val newestAccountData = it as? JSONObject
            if (newestAccountData != null && newestAccountData.has("id") && newestAccountData.has("name")) {
                val account_options = newestAccountData.getJSONObject("options")
                if (account_options.getString("memo_key") == newKey) {
                    showToast(resources.getString(R.string.kVcPermissionSubmitTipsMemoKeyNoChanged))
                } else {
                    (_ctx as Activity).guardWalletUnlocked(false) { unlocked ->
                        if (unlocked) {
                            _onModifyMemoKeyCore(permissionItem, newKey, newestAccountData)
                        }
                    }
                }
            } else {
                showToast(resources.getString(R.string.tip_network_error))
            }
            return@then null
        }
    }

    private fun _onModifyMemoKeyCore(permissionItem: JSONObject, newKey: String, account_data: JSONObject) {
        val uid = account_data.getString("id")
        val account_options = account_data.getJSONObject("options")
        val op_data = JSONObject().apply {
            put("fee", JSONObject().apply {
                put("amount", 0)
                put("asset_id", ChainObjectManager.sharedChainObjectManager().grapheneCoreAssetID)
            })
            put("account", uid)
            put("new_options", JSONObject().apply {
                put("memo_key", newKey)
                put("voting_account", account_options.getString("voting_account"))
                put("num_witness", account_options.getInt("num_witness"))
                put("num_committee", account_options.getInt("num_committee"))
                put("votes", account_options.getJSONArray("votes"))
            })
        }

        //  ?????????????????????????????????????????????????????????????????????
        (_ctx as Activity).GuardProposalOrNormalTransaction(EBitsharesOperations.ebo_account_update, false, false,
                op_data, account_data) { isProposal: Boolean, proposal_create_args: JSONObject? ->
            assert(!isProposal)
            val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), _ctx)
            mask.show()
            BitsharesClientManager.sharedBitsharesClientManager().accountUpdate(op_data).then {
                ChainObjectManager.sharedChainObjectManager().queryFullAccountInfo(uid).then {
                    mask.dismiss()
                    //  ??????
                    _refreshUI(it as? JSONObject)
                    showToast(resources.getString(R.string.kVcPermissionSubmitModifyMemoKeyFullOK))
                    //  [??????]
                    btsppLogCustom("txUpdateMemoKeyPermissionFullOK", jsonObjectfromKVS("account", uid))
                    return@then null
                }.catch {
                    mask.dismiss()
                    showToast(resources.getString(R.string.kVcPermissionSubmitModifyMemoKeyOK))
                    //  [??????]
                    btsppLogCustom("txUpdateMemoKeyPermissionOK", jsonObjectfromKVS("account", uid))
                }
                return@then null
            }.catch { err ->
                mask.dismiss()
                showGrapheneError(err)
                //  [??????]
                btsppLogCustom("txUpdateMemoKeyPermissionFailed", jsonObjectfromKVS("account", uid))
            }
        }
    }

    private fun onEditPermission(permissionItem: JSONObject) {
        if (permissionItem.optBoolean("is_memo")) {
            UtilsAlert.showInputBox(_ctx!!, resources.getString(R.string.kVcPermissionMemoKeyModifyAskTitle),
                    resources.getString(R.string.kVcPermissionMemoKeyModifyInputPlaceholder), is_password = false).then {
                if (it != null && it is String) {
                    val tfvalue = it
                    (_ctx as Activity).guardWalletUnlocked(false) { unlocked ->
                        if (unlocked) {
                            _onModifyMemoKeyClicked(permissionItem, tfvalue)
                        }
                    }
                }
            }
        } else {
            //  REMARK?????????????????????????????????
            val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), _ctx)
            mask.show()
            ChainObjectManager.sharedChainObjectManager().queryGlobalProperties().then {
                mask.dismiss()
                val gp = ChainObjectManager.sharedChainObjectManager().getObjectGlobalProperties()
                val maximum_authority_membership = gp.optJSONObject("parameters").getInt("maximum_authority_membership")
                //  ??????????????????
                val result_promise = Promise()
                (_ctx as Activity).goTo(ActivityPermissionEdit::class.java, true, args = JSONObject().apply {
                    put("permission", permissionItem)
                    put("maximum_authority_membership", maximum_authority_membership)
                    put("result_promise", result_promise)
                })
                result_promise.then {
                    //  ??????????????????
                    _refreshUI(it as? JSONObject)
                    return@then null
                }
                return@then null
            }.catch {
                mask.dismiss()
                showToast(resources.getString(R.string.tip_network_error))
            }
        }
    }

    /**
     *  (private) ????????????????????????????????????
     */
    private fun _canBeModified(account: JSONObject, permission: Any): Boolean {
        val oid = account.getString("id")
        if (oid == BTS_GRAPHENE_COMMITTEE_ACCOUNT ||
                oid == BTS_GRAPHENE_WITNESS_ACCOUNT ||
                oid == BTS_GRAPHENE_TEMP_ACCOUNT ||
                oid == BTS_GRAPHENE_PROXY_TO_SELF) {
            return false
        }
        return true
    }

    private fun _parsePermissionJson(permission: Any, title: String, account: JSONObject, type: EBitsharesPermissionType): JSONObject? {
        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val canBeModified = _canBeModified(account, permission)
        //  memo key
        if (permission is String) {
            return JSONObject().apply {
                put("title", title)
                put("weight_threshold", 1)
                put("type", type)
                put("is_memo", true)
                put("canBeModified", canBeModified)
                put("items", jsonArrayfrom(JSONObject().apply {
                    put("key", permission)
                    put("threshold", 1)
                }))
            }
        }
        //  other permission
        val permission_json = permission as JSONObject
        val weight_threshold = permission_json.getInt("weight_threshold")
        val account_auths = permission_json.getJSONArray("account_auths")
        val key_auths = permission_json.getJSONArray("key_auths")
        val address_auths = permission_json.getJSONArray("address_auths")

        val list = mutableListOf<JSONObject>()
        var onlyIncludeKeyAuthority = true
        var curr_threshold = 0

        account_auths.forEach<JSONArray> { item ->
            assert(item!!.length() == 2)
            val oid = item.getString(0)
            val threshold = item.getInt(1)
            curr_threshold += threshold

            val mutable_hash = JSONObject().apply {
                put("key", oid)
                put("isaccount", true)
                put("threshold", threshold)
            }

            //  ?????????????????????
            val multi_sign_account = chainMgr.getChainObjectByID(oid, true)
            if (multi_sign_account != null) {
                mutable_hash.put("name", multi_sign_account.getString("name"))
            }
            list.add(mutable_hash)
            onlyIncludeKeyAuthority = false
        }

        key_auths.forEach<JSONArray> { item ->
            assert(item!!.length() == 2)
            val key = item.getString(0)
            val threshold = item.getInt(1)
            curr_threshold += threshold
            list.add(JSONObject().apply {
                put("key", key)
                put("iskey", true)
                put("threshold", threshold)
            })
        }

        address_auths.forEach<JSONArray> { item ->
            assert(item!!.length() == 2)
            val addr = item.getString(0)
            val threshold = item.getInt(1)
            curr_threshold += threshold
            list.add(JSONObject().apply {
                put("key", addr)
                put("isaddr", true)
                put("threshold", threshold)
            })
            onlyIncludeKeyAuthority = false
        }

        if (curr_threshold >= weight_threshold) {
            //  ????????????????????????
            list.sortByDescending { it.getInt("threshold") }

            //  REMARK??????????????????????????????????????????KEY???????????????account???address??????
            val only_one_key = onlyIncludeKeyAuthority && list.size == 1
            return JSONObject().apply {
                put("title", title)
                put("weight_threshold", weight_threshold)
                put("type", type)
                put("only_one_key", only_one_key)
                put("items", list.toJsonArray())
                put("canBeModified", canBeModified)
                put("raw", permission_json)
            }
        }

        //  no permission
        return null
    }

    /*
     *  (private) ???????????????
     */
    private fun _initDataArrayWithFullAccountData(full_account_data: JSONObject) {
        _data = JSONArray().also { ary ->
            val account = full_account_data.getJSONObject("account")
            val owner = account.getJSONObject("owner")
            val active = account.getJSONObject("active")
            val memo_key = account.getJSONObject("options").getString("memo_key")
            _parsePermissionJson(owner, resources.getString(R.string.kVcPermissionTypeOwner), account, EBitsharesPermissionType.ebpt_owner)?.let {
                ary.put(it)
            }
            _parsePermissionJson(active, resources.getString(R.string.kVcPermissionTypeActive), account, EBitsharesPermissionType.ebpt_active)?.let {
                ary.put(it)
            }
            _parsePermissionJson(memo_key, resources.getString(R.string.kVcPermissionTypeMemo), account, EBitsharesPermissionType.ebpt_memo)?.let {
                ary.put(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val self = this
        _ctx = inflater.context
        _view = inflater.inflate(R.layout.fragment_permission_list, container, false)
        //  ?????????UI
        _refreshUI(WalletManager.sharedWalletManager().getWalletAccountInfo()!!)
        //  ????????????
        _view?.findViewById<Button>(R.id.btn_edit_password)?.setOnClickListener {
            activity?.goTo(ActivityNewAccountPassword::class.java, true, args = JSONObject().apply {
                put("title", self.resources.getString(R.string.kVcTitleEditPassword))
                put("scene", kNewPasswordSceneChangePassowrd)
            })
        }
        return _view
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentPermissionList.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                FragmentPermissionList().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
