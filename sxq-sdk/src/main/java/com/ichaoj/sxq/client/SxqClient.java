package com.ichaoj.sxq.client;

import com.ichaoj.sxq.client.beans.*;
import com.ichaoj.sxq.client.compoment.ResultBase;
import com.ichaoj.sxq.client.compoment.ResultInfo;
import com.ichaoj.sxq.client.compoment.StoreResult;
import com.ichaoj.sxq.client.compoment.YclNetUtil;
import com.ichaoj.sxq.client.enums.Env;
import com.ichaoj.sxq.client.enums.StoreVisibleEnum;
import com.ichaoj.sxq.client.enums.SxqServiceEnum;
import com.yiji.openapi.tool.fastjson.JSONObject;
import com.yiji.openapi.tool.util.DigestUtil;
import com.yiji.openapi.tool.util.DigestUtil.DigestALGEnum;
import org.apache.commons.lang3.StringUtils;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SxqClient {
    private String appKey;
    private String appSecret;
    private Env env;

    public SxqClient(String appKey, String appSecret, Env env) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.env = env;
    }

    public ResultBase ping() {
        ResultBase resultBase = new ResultBase();
        try {
            String json = YclNetUtil.doPost(this.env.getCode() + SxqServiceEnum.PING.getCode(), null, 30000, 30000, getBaseHeader());
            JSONObject jsonObject = JSONObject.parseObject(json);
            resultBase.setSuccess(jsonObject.getBooleanValue("success"));
            resultBase.setMessage(jsonObject.getString("message"));
            return resultBase;
        } catch (IOException e) {
            resultBase.setMessage(e.getMessage());
            return resultBase;
        }
    }

    public StoreResult filePreservation(StoreApiOrder storeApiOrder) {
        StoreResult resultBase = new StoreResult();
        Map<String, String> params = new HashMap<>();
        storeApiOrder.check();
        //设置参数=================================================================
        params.put("fileName", storeApiOrder.getFileName());
        params.put("storeName", storeApiOrder.getStoreName());
        params.put("fileBase64", storeApiOrder.getFileBase64());
        params.put("isPublic", storeApiOrder.getIsPublic());
        //设置参数=================================================================
        String signStr = DigestUtil.digest(params, this.appSecret, DigestALGEnum.MD5);
        params.put("sign", signStr);
        try {
            String json = YclNetUtil.doPost(env.getCode() + SxqServiceEnum.STORE.getCode(), params, 30000, 30000, getBaseHeader());

            JSONObject jsonObject = JSONObject.parseObject(json);
            resultBase.setSuccess(jsonObject.getBooleanValue("success"));
            resultBase.setMessage(jsonObject.getString("message"));
            resultBase.setContractId(jsonObject.getString("storeNo"));
            return resultBase;
        } catch (IOException e) {
            resultBase.setMessage(e.getMessage());
            return resultBase;
        }
    }

    /**
     * 个人|企业 实名认证
     */
    public ResultInfo realNameCertification(UserRealNameInfo userRealNameInfo) {
        String realNameUrl = env.getCode() + SxqServiceEnum.REALNAMEAUTH.getCode();
        return certificationByUrl(userRealNameInfo, realNameUrl);
    }

    /**
     * 重新认证企业信息
     */
    public ResultInfo enterpriseReCertification(UserRealNameInfo userRealNameInfo) {
        if (!"JG".equals(userRealNameInfo.getType())) {
            return ResultInfo.error("type mast be JG");
        }
        String recertUrl = env.getCode() + SxqServiceEnum.ENTERPRISE_REALNAME_REAUTH.getCode();
        return certificationByUrl(userRealNameInfo, recertUrl);
    }

    /**
     * 生成用户认证信息, 并请求认证接口
     */
    private ResultInfo certificationByUrl(UserRealNameInfo userRealNameInfo, String url) {
        ResultInfo resultBase = new ResultInfo();
        try {
            userRealNameInfo.check();
        } catch (Exception e) {
            resultBase.setSuccess(false);
            resultBase.setMessage(e.getMessage());
            return resultBase;
        }

        Map<String, String> params = new HashMap<>();
        params.put("certNo", userRealNameInfo.getCertNo());
        params.put("realName", userRealNameInfo.getRealName());
        if (StringUtils.isNotBlank(userRealNameInfo.getEnterpriseRealName())) {
            params.put("enterpriseRealName", userRealNameInfo.getEnterpriseRealName());
        }
        if (StringUtils.isNotBlank(userRealNameInfo.getEnterpriseCertType())) {
            params.put("enterpriseCertType", userRealNameInfo.getEnterpriseCertType());
        }
        if (StringUtils.isNotEmpty(userRealNameInfo.getEnterpriseCertNo())) {
            params.put("enterpriseCertNo", userRealNameInfo.getEnterpriseCertNo());
        }
        if (StringUtils.isNotBlank(userRealNameInfo.getMobile())) {
            params.put("mobile", userRealNameInfo.getMobile());
        }
        if (StringUtils.isNotBlank(userRealNameInfo.getMail())) {
            params.put("mail", userRealNameInfo.getMail());
        }
        params.put("type", userRealNameInfo.getType());

        String signStr = DigestUtil.digest(params, this.appSecret, DigestALGEnum.MD5);
        params.put("sign", signStr);
        try {
            String reponse = YclNetUtil.doPost(url, params, 0, 0, getBaseHeader());
            JSONObject jsonObject = JSONObject.parseObject(reponse);
            resultBase.setSuccess(jsonObject.getBooleanValue("success"));
            resultBase.setMessage(jsonObject.getString("message"));
            return resultBase;
        } catch (Exception e) {
            resultBase.setSuccess(false);
            resultBase.setMessage(e.getMessage());
            return resultBase;
        }
    }

    public StoreResult signatory(SignatoryApiOrder order) {
        StoreResult resultBase = new StoreResult();
        try {
            order.check();
        } catch (Exception e) {
            resultBase.setMessage(e.getMessage());
            return resultBase;
        }

        Map<String, String> params = new HashMap<>();
        params.put("pdfFileBase64", order.getPdfFileBase64());
        params.put("yclDataStore.userBizNumber", order.getSxqDataStore().getUserBizNumber());
        params.put("yclDataStore.storeName", order.getSxqDataStore().getStoreName());
        params.put("yclDataStore.isPublic", order.getSxqDataStore().getIsPublic());
        params.put("yclDataStore.callBackUrl", order.getSxqDataStore().getCallBackUrl());

        for (int i = 0; i < order.getSxqSignatoryList().size(); i++) {

            if (order.getRealNameMask() != null) {
                order.getSxqSignatoryList().get(i).setRealNameMask(order.getRealNameMask());
            }
            if (order.getCertNoMask() != null) {
                order.getSxqSignatoryList().get(i).setCertNoMask(order.getCertNoMask());
            }
            // 设置必填参数
            params.put("yclSignatoryList[" + i + "].realName", order.getSxqSignatoryList().get(i).getRealName());
            params.put("yclSignatoryList[" + i + "].sealType", order.getSxqSignatoryList().get(i).getSealType());
            params.put("yclSignatoryList[" + i + "].signatoryAuto", order.getSxqSignatoryList().get(i).getSignatoryAuto());
            params.put("yclSignatoryList[" + i + "].signatoryUserType", order.getSxqSignatoryList().get(i).getSignatoryUserType());
            params.put("yclSignatoryList[" + i + "].signatoryTime", order.getSxqSignatoryList().get(i).getSignatoryTime());
            params.put("yclSignatoryList[" + i + "].groupName", order.getSxqSignatoryList().get(i).getGroupName());
            params.put("yclSignatoryList[" + i + "].groupChar", order.getSxqSignatoryList().get(i).getGroupChar());
            // 设置可选参数


            if (order.getSxqSignatoryList().get(i).getEmail() != null) {
                params.put("yclSignatoryList[" + i + "].email", order.getSxqSignatoryList().get(i).getEmail());
            }
            if (order.getSxqSignatoryList().get(i).getPhone() != null) {
                params.put("yclSignatoryList[" + i + "].phone", order.getSxqSignatoryList().get(i).getPhone());
            }
            if (StringUtils.isNotEmpty(order.getSxqSignatoryList().get(i).getKeywords())) {
                params.put("yclSignatoryList[" + i + "].keywords", order.getSxqSignatoryList().get(i).getKeywords());
            }
            if (order.getSxqSignatoryList().get(i).getSignatureX() != null) {
                params.put("yclSignatoryList[" + i + "].signatureX", String.valueOf(order.getSxqSignatoryList().get(i).getSignatureX()));
            }
            if (order.getSxqSignatoryList().get(i).getSignatureY() != null) {
                params.put("yclSignatoryList[" + i + "].signatureY", String.valueOf(order.getSxqSignatoryList().get(i).getSignatureY()));
            }
            if (order.getSxqSignatoryList().get(i).getSignaturePage() != null) {
                params.put("yclSignatoryList[" + i + "].signaturePage", String.valueOf(order.getSxqSignatoryList().get(i).getSignaturePage()));
            }

            if (order.getSxqSignatoryList().get(i).getCertNo() != null) {
                params.put("yclSignatoryList[" + i + "].certNo", order.getSxqSignatoryList().get(i).getCertNo());
                params.put("yclSignatoryList[" + i + "].certType", order.getSxqSignatoryList().get(i).getCertType());
            }

            if (order.getSxqSignatoryList().get(i).getSealPurpose() != null) {
                params.put("yclSignatoryList[" + i + "].sealPurpose", order.getSxqSignatoryList().get(i).getSealPurpose());
            }

            if (order.getSxqSignatoryList().get(i).getRealNameMask() != null) {
                params.put("yclSignatoryList[" + i + "].realNameMask", order.getSxqSignatoryList().get(i).getRealNameMask() + "");
            }

            if (order.getSxqSignatoryList().get(i).getCertNoMask() != null) {
                params.put("yclSignatoryList[" + i + "].certNoMask", order.getSxqSignatoryList().get(i).getCertNoMask() + "");
            }

            if (order.getSxqSignatoryList().get(i).getSealSn() != null) {
                params.put("yclSignatoryList[" + i + "].sealSn", order.getSxqSignatoryList().get(i).getSealSn());
            }

            // 如果设置了合同的截止时间，那么签约人的截止时间就设定为合同的截止时间，
            // 如果给单个签约人设定的截止时间，以给签约人设置的截止时间为准
            if (order.getSxqDataStore().getValidTimeStamp() != null) {
                params.put("yclSignatoryList[" + i + "].validTimeStamp", order.getSxqDataStore().getValidTimeStamp() + "");
            }
            if (order.getSxqSignatoryList().get(i).getValidTimeStamp() != null) {
                params.put("yclSignatoryList[" + i + "].validTimeStamp", order.getSxqSignatoryList().get(i).getValidTimeStamp() + "");
            }
        }

        String signStr = DigestUtil.digest(params, this.appSecret, DigestALGEnum.MD5);
        params.put("sign", signStr);
        try {
            String json = YclNetUtil.doPost(env.getCode() + SxqServiceEnum.SIGNATORY.getCode(), params, 0, 0, getBaseHeader());

            JSONObject jsonObject = JSONObject.parseObject(json);
            resultBase.setSuccess(jsonObject.getBooleanValue("success"));
            resultBase.setMessage(jsonObject.getString("message"));
            JSONObject data = (JSONObject) jsonObject.get("data");
            resultBase.setContractId(data.getString("contractId"));
            resultBase.setSignUrl(data.getString("signUrl"));
            return resultBase;
        } catch (IOException e) {
            resultBase.setMessage(e.getMessage());
            return resultBase;
        }
    }

    /**
     * 云存文件取回
     *
     * @param contractId 存储编号
     */
    public ResultInfo downloadFile(String contractId) {
        Map<String, String> params = new HashMap<>(3);
        params.put("contractId", contractId);
        try {
            String s = YclNetUtil.doPost(env.getCode() + SxqServiceEnum.FILE_DOWNLOAD.getCode(), params, 60000, 60000, getBaseHeader());
            return JSONObject.parseObject(s, ResultInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author SOFAS
     * @description         数据存证
     * @date  2019/9/19
      * @param ocsvs        待存证的数据
     * @param storeId       最佳存证信息的存证编号（有存证编号时会对已有的存证主体进行追加操作，没有则会创建一个新的存证主体）
     * @param isPublic      是否公开（选择公开则被查询时会直接显示所有信息，否则将会对关键信息脱敏）
     * @param callback      追加存证成功后的回调接口（没有时存证成功后不会自动回调，需要手动到查询小插件查看是否已存证上链成功）
     * @param storeName     存证名称
     * @return com.ichaoj.ycl.client.compoment.ResultInfo
     **/
    public ResultInfo ocsv(List<Ocsv> ocsvs, Long storeId, String isPublic, String callback, String storeName){
        if (ocsvs == null || ocsvs.size() == 0){
            return ResultInfo.error("需要存证的数据不能为空");
        }
        if (this.appKey == null || this.appKey.length() == 0){
            return ResultInfo.error("appKey不能为空");
        }
        if (this.appSecret == null || this.appSecret.length() == 0){
            return ResultInfo.error("appSecret不能为空");
        }
        isPublic = StoreVisibleEnum.PUBLIC.getCode().equals(isPublic) ? StoreVisibleEnum.PUBLIC.getCode() : StoreVisibleEnum.PRIVATE.getCode();
        String s = ocsvDeal(ocsvs);
        return YclNetUtil.ocsv(new OcsvRequst(this.appKey, this.appSecret, env.getCode(), storeId, s, isPublic, callback, storeName));
    }

    public ResultInfo queryCustomizedLogo(){
        try {
            String res = YclNetUtil.doPost(env.getCode() + SxqServiceEnum.QUERY_CUSTOMIZED_LOGO.getCode(), null, 60000, 60000, getBaseHeader());
            return JSONObject.parseObject(res, ResultInfo.class);
        } catch (IOException e) {
            return ResultInfo.error(e.getMessage());
        }
    }

    public ResultInfo setCustomizedLogo(String logoPath){
        if (StringUtils.isBlank(logoPath)){
            return ResultInfo.error("logo文件路径不能为空");
        }
        try {
            HashMap<String, String> params = new HashMap<>();
            byte[] bytes = Files.readAllBytes(Paths.get(logoPath));
            BASE64Encoder encoder = new BASE64Encoder();
            String logoBase64 = encoder.encodeBuffer(bytes);
            params.put("logoBase64", logoBase64);
            String res = YclNetUtil.doPost(env.getCode() + SxqServiceEnum.SET_CUSTOMIZED_LOGO.getCode(), params, 60000, 60000, getBaseHeader());
            return JSONObject.parseObject(res, ResultInfo.class);
        } catch (IOException e) {
            return ResultInfo.error(e.getMessage());
        }
    }

    private String ocsvDeal(List<Ocsv> l){
        for (Ocsv o : l){
            List<Ocsv> sb = o.getSubOcsv();
            if (sb != null && sb.size() != 0){
                String s = ocsvDeal(sb);
                o.setSubOcsvStr(s);
            }
        }
        return JSONObject.toJSONString(l);
    }

    private HashMap<String, String> getBaseHeader(){
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("x-sxq-open-accesstoken", this.appKey);
        headerMap.put("x-sxq-open-accesssecret", this.appSecret);
        return headerMap;
    }
}
