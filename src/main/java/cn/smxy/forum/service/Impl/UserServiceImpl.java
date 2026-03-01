package cn.smxy.forum.service.Impl;

import cn.smxy.forum.domain.entity.LoginUser;
import cn.smxy.forum.domain.entity.User;
import cn.smxy.forum.domain.other.UserManagerDetail;
import cn.smxy.forum.domain.param.query.UserPageListDTO;
import cn.smxy.forum.domain.param.update.UpdateUserCenterDetailDTO;
import cn.smxy.forum.domain.param.update.UpdateUserDTO;
import cn.smxy.forum.domain.vo.UserCenterListVo;
import cn.smxy.forum.domain.vo.UserManagerPageListVo;
import cn.smxy.forum.mapper.UserMapper;
import cn.smxy.forum.mapping.UserMapping;
import cn.smxy.forum.service.ICommentService;
import cn.smxy.forum.service.IConcernService;
import cn.smxy.forum.service.IPostService;
import cn.smxy.forum.service.IUserService;
import cn.smxy.forum.utils.R;
import cn.smxy.forum.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static cn.smxy.forum.constant.Constants.NO_DELETE;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private IConcernService concernService;
    @Autowired
    private ICommentService commentService;
    @Autowired
    private IPostService postService;
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<UserManagerPageListVo> getUserPageList(UserPageListDTO userPageListDTO) {
        return userMapper.getUserPageList(userPageListDTO);
    }

    @Override
    public UserManagerDetail getUserDetailById(Long userId) {
        return userMapper.getUserDetailById(userId);
    }

    @Override
    public Integer updateUser(UpdateUserDTO updateUserDTO, String updateBy) {
        if(userMapper.updateUser(updateUserDTO,updateBy)>0){
            updateRedisUser(updateUserDTO.getUserId());
            return 1;
        }else {
            return 0;
        }
    }

    @Override
    public Integer deleteUserRole(Long userId) {
        return userMapper.deleteUserRole(userId);
    }

    @Override
    public Integer addUserRoles(Long userId, List<Long> roleIds) {
        return userMapper.addUserRoles(userId, roleIds);
    }

    @Override
    public boolean checkUserName(Long userId, String userName) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUserName, userName);
        lqw.ne(User::getUserId, userId);
        lqw.eq(User::getDelFlag,0);
        return userMapper.selectCount(lqw) > 0;
    }

    @Override
    public boolean checkNickName(Long userId, String nickName) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getNickName, nickName);
        lqw.ne(User::getUserId, userId);
        lqw.eq(User::getDelFlag,0);
        return userMapper.selectCount(lqw) > 0;
    }

    @Override
    public boolean checkEmail(Long userId, String email) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getEmail, email);
        lqw.ne(User::getUserId, userId);
        lqw.eq(User::getDelFlag,0);
        return userMapper.selectCount(lqw) > 0;
    }

    @Override
    public boolean updatePassword(Long userId, String oldpassword, String newPassword) {
        User user = userMapper.selectById(userId);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getUserName(),oldpassword);
        try {
            Authentication authenticate = authenticationManager.authenticate(token);
        }catch (BadCredentialsException e){
            return false;
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(newPassword));
        return userMapper.updateById(user)>0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserPoints(Long userId, Integer points) {
        User user = userMapper.selectById(userId);
        user.setPoints(user.getPoints()+points);
        return userMapper.updateById(user)>0;
    }

    @Override
    public UserCenterListVo getUserCenterDetail(Long userId) {

        User user = userMapper.selectById(userId);
        log.error(user+"");
        UserCenterListVo userCenterListVo = UserMapping.INSTANCE.toUserCenterListVo(user);
        log.error(userCenterListVo+"");

        userCenterListVo.setConcernTotal(concernService.getConcernCount(userId));
        userCenterListVo.setFanTotal(concernService.getFansCount(userId));
        userCenterListVo.setConcernListVos(concernService.getUserCenterConcernList(userId));
        userCenterListVo.setFansListVos(concernService.getUserCenterFansList(userId));
        userCenterListVo.setCommentNumber(commentService.getCommentCount(userId));
        userCenterListVo.setPostNumber(postService.getPostCount(userId));


        return userCenterListVo;
    }

    @Override
    public void updateRedisUser(Long userId) {
        User user = userMapper.selectById(userId);
        LoginUser loginUser = redisUtil.getCacheObject("user:" + userId);
        if(loginUser==null){
            return;
        }
        loginUser.setUser(user);
        redisUtil.setCacheObject("user:" + userId, loginUser);
    }

    @Override
    public boolean updateUserCenterDetail(Long userId, UpdateUserCenterDetailDTO updateUserCenterDetailDTO) {
        User user = userMapper.selectById(userId);
        user.setAvatar(updateUserCenterDetailDTO.getAvatar());
        user.setNickName(updateUserCenterDetailDTO.getNickName());
        user.setSignature(updateUserCenterDetailDTO.getSignature());
        if(userMapper.updateById(user)>0){
            updateRedisUser(userId);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public R updateUserEmail(Long userId, String email) {
        User user = userMapper.selectById(userId);
        if(user.getEmail().equals(email)){
            return R.fail("新邮箱不能与旧邮箱一致");
        }else{
            LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
            lqw.eq(User::getEmail, email)
                    .eq(User::getDelFlag,NO_DELETE);
            if(userMapper.selectOne(lqw)!=null){
                return R.fail("该邮箱已被使用");
            }else {
                user.setEmail(email);
                return R.to(userMapper.updateById(user)>0,"修改");
            }
        }
    }

    @Override
    public R updateUserBackground(Long userId, String background) {
        User user = userMapper.selectById(userId);
        user.setBackground(background);
        if(userMapper.updateById(user)>0){
            updateRedisUser(userId);
            return R.ok();
        }else{
            return R.fail();
        }
    }

}
