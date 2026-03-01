package cn.smxy.forum.controller;

import cn.smxy.forum.domain.other.RoleNameList;
import cn.smxy.forum.domain.other.TableDataInfo;
import cn.smxy.forum.domain.other.UserManagerDetail;
import cn.smxy.forum.domain.param.other.PageQuery;
import cn.smxy.forum.domain.param.query.UserPageListDTO;
import cn.smxy.forum.domain.param.update.UpdateUserDTO;
import cn.smxy.forum.domain.vo.UserManagerDetailVo;
import cn.smxy.forum.domain.vo.UserManagerPageListVo;
import cn.smxy.forum.mapping.RoleMapping;
import cn.smxy.forum.service.IRoleService;
import cn.smxy.forum.service.IUserService;
import cn.smxy.forum.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/userManager")
@Api(tags = "用户管理")
public class UserManagerController extends BaseController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IRoleService roleService;

    @GetMapping("/getUserPageList")
    @ApiOperation(("获取用户管理分页数据"))
    @PreAuthorize("@myExpressionUtil.myAuthority('userManager:pagelist')")
    public TableDataInfo<List<UserManagerPageListVo>> getUserPageList(@Validated PageQuery pageQuery, UserPageListDTO userPageListDTO){
        startPage();
        List<UserManagerPageListVo> userManagerPageListVos = userService.getUserPageList(userPageListDTO);

        return getDataTable(userManagerPageListVos);
    }

    @GetMapping("/detail/{userId}")
    @ApiOperation("获取当前用户详情")
    @PreAuthorize("@myExpressionUtil.myAuthority('userManager:detail')")
    public R<UserManagerDetailVo> getUserDetailById(@PathVariable("userId") Long userId){
        UserManagerDetail userManagerDetail=userService.getUserDetailById(userId);


        UserManagerDetailVo userManagerDetailVo=new UserManagerDetailVo();
        userManagerDetailVo.setUserManagerDetail(userManagerDetail);
        List<RoleNameList> roleNameList = RoleMapping.INSTANCE.toRoleNameListVoList(roleService.list());

        userManagerDetailVo.setRoleNameList(roleNameList);

        return R.ok(userManagerDetailVo);
    }

    @PutMapping("/update")
    @ApiOperation("编辑用户信息")
    @PreAuthorize("@myExpressionUtil.myAuthority('userManager:put')")
    public R updateUser(@RequestBody @Validated UpdateUserDTO updateUserDTO){
        Long userId=updateUserDTO.getUserId();

        if(userService.checkUserName(userId,updateUserDTO.getUserName())){
            return R.fail("该账号已存在");
        }
        if(userService.checkNickName(userId,updateUserDTO.getNickName())){
            return R.fail("该昵称已存在");
        }
        if(userService.checkEmail(userId,updateUserDTO.getEmail())){
            return R.fail("该邮箱已存在");
        }

        if(userService.updateUser(updateUserDTO,getUsername())>0){
            userService.deleteUserRole(updateUserDTO.getUserId());
            if(updateUserDTO.getRoleIds().isEmpty()){
                return R.ok();
            }
            return userService.addUserRoles(updateUserDTO.getUserId(),updateUserDTO.getRoleIds())>0?R.ok():R.fail("修改用户权限失败");
        }else {
            return R.fail("修改失败");
        }
    }
}
