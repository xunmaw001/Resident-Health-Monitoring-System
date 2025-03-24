
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 健康科普
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/kepuzhishi")
public class KepuzhishiController {
    private static final Logger logger = LoggerFactory.getLogger(KepuzhishiController.class);

    private static final String TABLE_NAME = "kepuzhishi";

    @Autowired
    private KepuzhishiService kepuzhishiService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private GerentizhengService gerentizhengService;//健康上报
    @Autowired
    private KepuzhishiCollectionService kepuzhishiCollectionService;//科普收藏
    @Autowired
    private KepuzhishiLiuyanService kepuzhishiLiuyanService;//科普留言
    @Autowired
    private NewsService newsService;//公告信息
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        params.put("kepuzhishiDeleteStart",1);params.put("kepuzhishiDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = kepuzhishiService.queryPage(params);

        //字典表数据转换
        List<KepuzhishiView> list =(List<KepuzhishiView>)page.getList();
        for(KepuzhishiView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KepuzhishiEntity kepuzhishi = kepuzhishiService.selectById(id);
        if(kepuzhishi !=null){
            //entity转view
            KepuzhishiView view = new KepuzhishiView();
            BeanUtils.copyProperties( kepuzhishi , view );//把实体数据重构到view中
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody KepuzhishiEntity kepuzhishi, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,kepuzhishi:{}",this.getClass().getName(),kepuzhishi.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<KepuzhishiEntity> queryWrapper = new EntityWrapper<KepuzhishiEntity>()
            .eq("kepuzhishi_name", kepuzhishi.getKepuzhishiName())
            .eq("kepuzhishi_types", kepuzhishi.getKepuzhishiTypes())
            .eq("kepuzhishi_video", kepuzhishi.getKepuzhishiVideo())
            .eq("zan_number", kepuzhishi.getZanNumber())
            .eq("cai_number", kepuzhishi.getCaiNumber())
            .eq("kepuzhishi_delete", kepuzhishi.getKepuzhishiDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KepuzhishiEntity kepuzhishiEntity = kepuzhishiService.selectOne(queryWrapper);
        if(kepuzhishiEntity==null){
            kepuzhishi.setKepuzhishiDelete(1);
            kepuzhishi.setInsertTime(new Date());
            kepuzhishi.setCreateTime(new Date());
            kepuzhishiService.insert(kepuzhishi);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody KepuzhishiEntity kepuzhishi, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,kepuzhishi:{}",this.getClass().getName(),kepuzhishi.toString());
        KepuzhishiEntity oldKepuzhishiEntity = kepuzhishiService.selectById(kepuzhishi.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
        if("".equals(kepuzhishi.getKepuzhishiPhoto()) || "null".equals(kepuzhishi.getKepuzhishiPhoto())){
                kepuzhishi.setKepuzhishiPhoto(null);
        }
        if("".equals(kepuzhishi.getKepuzhishiVideo()) || "null".equals(kepuzhishi.getKepuzhishiVideo())){
                kepuzhishi.setKepuzhishiVideo(null);
        }

            kepuzhishiService.updateById(kepuzhishi);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<KepuzhishiEntity> oldKepuzhishiList =kepuzhishiService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<KepuzhishiEntity> list = new ArrayList<>();
        for(Integer id:ids){
            KepuzhishiEntity kepuzhishiEntity = new KepuzhishiEntity();
            kepuzhishiEntity.setId(id);
            kepuzhishiEntity.setKepuzhishiDelete(2);
            list.add(kepuzhishiEntity);
        }
        if(list != null && list.size() >0){
            kepuzhishiService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<KepuzhishiEntity> kepuzhishiList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            KepuzhishiEntity kepuzhishiEntity = new KepuzhishiEntity();
//                            kepuzhishiEntity.setKepuzhishiName(data.get(0));                    //标题 要改的
//                            kepuzhishiEntity.setKepuzhishiPhoto("");//详情和图片
//                            kepuzhishiEntity.setKepuzhishiTypes(Integer.valueOf(data.get(0)));   //科普类型 要改的
//                            kepuzhishiEntity.setKepuzhishiVideo(data.get(0));                    //科普视频 要改的
//                            kepuzhishiEntity.setZanNumber(Integer.valueOf(data.get(0)));   //赞 要改的
//                            kepuzhishiEntity.setCaiNumber(Integer.valueOf(data.get(0)));   //踩 要改的
//                            kepuzhishiEntity.setKepuzhishiContent("");//详情和图片
//                            kepuzhishiEntity.setKepuzhishiDelete(1);//逻辑删除字段
//                            kepuzhishiEntity.setInsertTime(date);//时间
//                            kepuzhishiEntity.setCreateTime(date);//时间
                            kepuzhishiList.add(kepuzhishiEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        kepuzhishiService.insertBatch(kepuzhishiList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }



    /**
    * 个性推荐
    */
    @IgnoreAuth
    @RequestMapping("/gexingtuijian")
    public R gexingtuijian(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("gexingtuijian方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        CommonUtil.checkMap(params);
        List<KepuzhishiView> returnKepuzhishiViewList = new ArrayList<>();

        //查看收藏
        Map<String, Object> params1 = new HashMap<>(params);params1.put("sort","id");params1.put("yonghuId",request.getSession().getAttribute("userId"));
        PageUtils pageUtils = kepuzhishiCollectionService.queryPage(params1);
        List<KepuzhishiCollectionView> collectionViewsList =(List<KepuzhishiCollectionView>)pageUtils.getList();
        Map<Integer,Integer> typeMap=new HashMap<>();//购买的类型list
        for(KepuzhishiCollectionView collectionView:collectionViewsList){
            Integer kepuzhishiTypes = collectionView.getKepuzhishiTypes();
            if(typeMap.containsKey(kepuzhishiTypes)){
                typeMap.put(kepuzhishiTypes,typeMap.get(kepuzhishiTypes)+1);
            }else{
                typeMap.put(kepuzhishiTypes,1);
            }
        }
        List<Integer> typeList = new ArrayList<>();//排序后的有序的类型 按最多到最少
        typeMap.entrySet().stream().sorted((o1, o2) -> o2.getValue() - o1.getValue()).forEach(e -> typeList.add(e.getKey()));//排序
        Integer limit = Integer.valueOf(String.valueOf(params.get("limit")));
        for(Integer type:typeList){
            Map<String, Object> params2 = new HashMap<>(params);params2.put("kepuzhishiTypes",type);
            PageUtils pageUtils1 = kepuzhishiService.queryPage(params2);
            List<KepuzhishiView> kepuzhishiViewList =(List<KepuzhishiView>)pageUtils1.getList();
            returnKepuzhishiViewList.addAll(kepuzhishiViewList);
            if(returnKepuzhishiViewList.size()>= limit) break;//返回的推荐数量大于要的数量 跳出循环
        }
        //正常查询出来商品,用于补全推荐缺少的数据
        PageUtils page = kepuzhishiService.queryPage(params);
        if(returnKepuzhishiViewList.size()<limit){//返回数量还是小于要求数量
            int toAddNum = limit - returnKepuzhishiViewList.size();//要添加的数量
            List<KepuzhishiView> kepuzhishiViewList =(List<KepuzhishiView>)page.getList();
            for(KepuzhishiView kepuzhishiView:kepuzhishiViewList){
                Boolean addFlag = true;
                for(KepuzhishiView returnKepuzhishiView:returnKepuzhishiViewList){
                    if(returnKepuzhishiView.getId().intValue() ==kepuzhishiView.getId().intValue()) addFlag=false;//返回的数据中已存在此商品
                }
                if(addFlag){
                    toAddNum=toAddNum-1;
                    returnKepuzhishiViewList.add(kepuzhishiView);
                    if(toAddNum==0) break;//够数量了
                }
            }
        }else {
            returnKepuzhishiViewList = returnKepuzhishiViewList.subList(0, limit);
        }

        for(KepuzhishiView c:returnKepuzhishiViewList)
            dictionaryService.dictionaryConvert(c, request);
        page.setList(returnKepuzhishiViewList);
        return R.ok().put("data", page);
    }

    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = kepuzhishiService.queryPage(params);

        //字典表数据转换
        List<KepuzhishiView> list =(List<KepuzhishiView>)page.getList();
        for(KepuzhishiView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KepuzhishiEntity kepuzhishi = kepuzhishiService.selectById(id);
            if(kepuzhishi !=null){


                //entity转view
                KepuzhishiView view = new KepuzhishiView();
                BeanUtils.copyProperties( kepuzhishi , view );//把实体数据重构到view中

                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody KepuzhishiEntity kepuzhishi, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,kepuzhishi:{}",this.getClass().getName(),kepuzhishi.toString());
        Wrapper<KepuzhishiEntity> queryWrapper = new EntityWrapper<KepuzhishiEntity>()
            .eq("kepuzhishi_name", kepuzhishi.getKepuzhishiName())
            .eq("kepuzhishi_types", kepuzhishi.getKepuzhishiTypes())
            .eq("kepuzhishi_video", kepuzhishi.getKepuzhishiVideo())
            .eq("zan_number", kepuzhishi.getZanNumber())
            .eq("cai_number", kepuzhishi.getCaiNumber())
            .eq("kepuzhishi_delete", kepuzhishi.getKepuzhishiDelete())
//            .notIn("kepuzhishi_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KepuzhishiEntity kepuzhishiEntity = kepuzhishiService.selectOne(queryWrapper);
        if(kepuzhishiEntity==null){
                kepuzhishi.setZanNumber(1);
                kepuzhishi.setCaiNumber(1);
            kepuzhishi.setKepuzhishiDelete(1);
            kepuzhishi.setInsertTime(new Date());
            kepuzhishi.setCreateTime(new Date());
        kepuzhishiService.insert(kepuzhishi);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

