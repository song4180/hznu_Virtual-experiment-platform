package com.dockers.docker.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dockers.docker.dao.ClassExperimentMapper;
import com.dockers.docker.dao.ExperimentMapper;
import com.dockers.docker.dao.ExperimentRecordMapper;
import com.dockers.docker.dao.UserMapper;
import com.dockers.docker.dto.ExpStatusCheckDTO;
import com.dockers.docker.dto.ExperimentAdminDTO;
import com.dockers.docker.dto.ExperimentStuDTO;
import com.dockers.docker.dto.ShowExperimentDTO;
import com.dockers.docker.entity.ClassExperiment;
import com.dockers.docker.entity.Experiment;
import com.dockers.docker.entity.ExperimentRecord;
import com.dockers.docker.entity.User;
import com.dockers.docker.exception.BadRequestException;
import com.dockers.docker.exception.ServiceException;
import com.dockers.docker.param.CreateContainerCmdParam;
import com.dockers.docker.param.ExperimentCloseParam;
import com.dockers.docker.param.ExperimentOpenParam;
import com.dockers.docker.service.ExperimentService;
import com.dockers.docker.utils.DockerClientUtil;
import com.dockers.docker.vo.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * ????????????
 *
 * @author zgx
 */
@Slf4j
@Service("experimentService")
public class ExperimentServiceImpl implements ExperimentService {
    @Resource(name = "experimentMapper")
    private ExperimentMapper experimentMapper;
    @Resource(name = "classExperimentMapper")
    private ClassExperimentMapper classExperimentMapper;
    @Resource(name = "experimentRecord")
    private ExperimentRecordMapper recordMapper;
    @Resource(name = "userMapper")
    private UserMapper userMapper;

    @Autowired
    private DockerClientUtil dockerClientUtil;

    /**
     * ??????????????????????????????????????????
     *
     * @param pageNum  ??????????????????
     * @param pageSize ?????????????????????
     * @return ???????????????????????????????????????????????????experimentDetailVO????????????experimentDetailVO
     */
    @Override
    public ExperimentDetailVO queryAllExperiment(int pageNum, int pageSize, Long classId) {
        Page<ExperimentStuDTO> page = new Page<>(pageNum, pageSize);
        Page<ExperimentStuDTO> experiments = experimentMapper.queryPageVO(page, classId);
        ExperimentDetailVO experimentDetailVO = new ExperimentDetailVO();
        experimentDetailVO.setExperimentsData(experiments);
        return experimentDetailVO;
    }

    /**
     * ???????????????????????????
     *
     * @return ??????????????????
     */
    @Override
    public ShowAddExperimentVO queryAll(Long classId) {
        Assert.notNull(classId, "????????????");

        QueryWrapper<Experiment> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("experiment_title", "experiment_id");
        List<Experiment> list = experimentMapper.selectList(queryWrapper);

        Map<String, Object> map = new HashMap<>(2);
        map.put("class_id", classId);
        List<ClassExperiment> list1 = classExperimentMapper.selectByMap(map);

        List<ShowExperimentDTO> showExperiments = list.stream()
                .map(item -> new ShowExperimentDTO(item.getExperimentId(), item.getExperimentTitle()))
                .collect(Collectors.toList());
        List<Integer> integers = list1.stream().distinct().map(ClassExperiment::getExperimentId).collect(Collectors.toList());
        ShowAddExperimentVO showAddExperimentVO = new ShowAddExperimentVO();
        showAddExperimentVO.setExperimentDTOS(showExperiments);
        showAddExperimentVO.setIsSelected(integers);
        return showAddExperimentVO;
    }


    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param id     ??????id
     * @param status ??????????????????
     * @return ??????????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(int id, Long classId, int status) {
        ClassExperiment experiment = experimentMapper.queryById(id, classId);
        if (status != experiment.getIsClosed()) {
            experiment.setIsClosed(status > 0 ? 1 : 0);
        } else {
            throw new ServiceException("????????????????????????");
        }
        return experimentMapper.updateStateById(experiment.getExperimentId(),
                experiment.getIsClosed(), experiment.getClassId()) > 0;
    }


    /**
     * ??????????????????????????????
     *
     * @param adminId ???????????????
     * @param classId ????????????
     * @return ???????????????????????????
     */
    @Override
    public List<ExperimentAdminDTO> queryAdminExperiment(Integer adminId, Long classId) {
        return experimentMapper.queryAll(adminId, classId);
    }

    /**
     * ????????????docker????????????
     * @return ConcurDockerNumVO
     */
    @Override
    public ConcurDockerNumVO queryExperimentNum() {
        QueryWrapper<ExperimentRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("record_id");
        queryWrapper.eq("is_closed",0);
        return new ConcurDockerNumVO(recordMapper.selectCount(queryWrapper));
    }

    @Override
    public boolean deleteExperiment(Long classId, int experimentId) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("class_Id", classId);
        map.put("experiment_id", experimentId);
        if (queryClassExperiment(map)) {
            throw new BadRequestException("?????????????????????");
        }
        return classExperimentMapper.deleteByMap(map) > 0;
    }

    @Transactional(
            rollbackFor = Exception.class
    )
    @Async
    @Override
    public void insertExperiment(Experiment experiment) throws InterruptedException, IOException {
        DockerClient dockerClient = DockerClientUtil.connectDocker();

        Image image = dockerClientUtil.createImage(dockerClient, experiment.getImageId(), experiment.getImageName());

        if (!ObjectUtils.isEmpty(image)) {
            experiment.setImageId(image.getId().substring(7, 19));
            experimentMapper.insert(experiment);
        }
        dockerClient.close();
    }

    @Override
    public List<ExperimentForAdminVO> listAll() {
        LambdaQueryWrapper<Experiment> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.select(Experiment::getExperimentId, Experiment::getExperimentTitle, Experiment::getExperimentDescribe, Experiment::getExperimentTask
                , Experiment::getImageDescribe, Experiment::getImageId, Experiment::getImageName);
        List<Experiment> experimentList = experimentMapper.selectList(queryWrapper);
        return experimentList.stream()
                .map(experiment -> new ExperimentForAdminVO(experiment.getExperimentId(), experiment.getExperimentTitle(),
                        experiment.getExperimentDescribe(), experiment.getExperimentTask(), experiment.getImageId(),
                        experiment.getImageDescribe(), experiment.getImageName())).collect(Collectors.toList());
    }

    @Override
    public boolean updateExperiment(ExperimentForAdminVO experimentForAdminVO) {
        UpdateWrapper<Experiment> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("experiment_title", experimentForAdminVO.getExperimentTitle())
                .set("experiment_describe", experimentForAdminVO.getExperimentDescribe())
                .set("experiment_task", experimentForAdminVO.getExperimentTask())
                .set("image_name", experimentForAdminVO.getImageName())
                .set("image_describe", experimentForAdminVO.getExperimentDescribe())
                .eq("experiment_id", experimentForAdminVO.getExperimentId());
        return experimentMapper.update(null, updateWrapper) == 1;
    }

    /**
     * ??????????????????
     * ????????????id????????????
     * ??????????????? ???????????? ???????????? ??????ExperimentEnterVO???
     * ????????????id?????????id??????ExperimentRecord,???????????????????????????????????????ExperimentEnterVO???
     * @param userId ??????id
     * @return ??????ExperimentEnterVO
     */
    @Override
    public ExpStatusCheckDTO checkExperimentStatus(Integer userId) {
        QueryWrapper<ExperimentRecord> recordQueryWrapper = new QueryWrapper<>();
        recordQueryWrapper.select("experiment_id","container_id").eq("is_closed",0).eq("user_id",userId);
        ExperimentRecord record = recordMapper.selectOne(recordQueryWrapper);
        return  record == null ? null : new ExpStatusCheckDTO(record.getExperimentId(),record.getContainerId());
    }

    @Override
    public ExperimentEnterVO experimentEnter(int experimentId, int userId) {
        ExperimentEnterVO experimentEnterVO = queryExperiment(experimentId);
        ExperimentRecord expRecord = selectExpRecord(experimentId, userId);
        if (expRecord == null) {
            expRecord = new ExperimentRecord();
            expRecord.setIsClosed(1);
        }
        experimentEnterVO.setIsClosed(expRecord.getIsClosed());
        return experimentEnterVO;
    }

    /**
     * ??????????????????
     * ????????????id?????????id??????ExperimentRecord,??????record??????,???????????????????????????,
     * ???????????????????????????????????????2??????
     * ?????????????????????????????????,???????????????????????????
     * @param expOpenParam ??????????????????
     * @param ip ??????ip??????
     * @return ??????????????????
     */
    @Override
    public Long experimentOpen(ExperimentOpenParam expOpenParam, String ip) {
        Assert.notNull(expOpenParam, "??????????????????????????????");

        int expId = expOpenParam.getExperimentId();
        int userId = expOpenParam.getUserId();
        Date startTime;
        long endTime;
        ExperimentRecord expRecord = selectExpRecord(expId, userId);
        if (expRecord == null) {
            ExperimentRecord record = new ExperimentRecord();
            record.setUserId(userId);
            record.setExperimentId(expId);
            Experiment exp = experimentMapper.selectById(expId);
            String imageName = exp.getImageName();
            String imageId = exp.getImageId();
            String stuNum = userMapper.selectOne(new QueryWrapper<User>().select("user_student_number").eq("user_id", userId)).getUserStudentNumber();
            String containerName = stuNum + "_" + imageName;
            try{
                ServerSocket socket = new ServerSocket(0);
                int port = socket.getLocalPort();
                socket.close();
                DockerClient dockerClient = DockerClientUtil.connectDocker();
                CreateContainerResponse containers = dockerClientUtil.createContainers(dockerClient, new CreateContainerCmdParam(imageId,
                        80, port, containerName, 1073741824L, expOpenParam.getPixel()));
                record.setContainerId(containers.getId().substring(0, 12));
                record.setContainerName(containerName);
                record.setOccupyPort(dockerClientUtil.getDockerHost() + ":" + port);
                record.setRecordIp(ip);
                startTime = new Date();
                endTime = startTime.getTime() + 3600 * 2 * 1000;
                record.setCreateTime(startTime);
                record.setEndTime(new Date(endTime));
                record.setIsClosed(1);
                recordMapper.insert(record);
                dockerClient.close();
                return endTime;
            }catch (IOException e){
                throw new ServiceException("???????????????????????????ip???"+ip+"?????????"+e.getMessage());
            }
        }else{
            int status = expRecord.getIsClosed();
            if (status == 0) {
                //????????????
                endTime = expRecord.getEndTime().getTime();
            } else {
                endTime = expRecord.getEndTime().getTime();
            }
            return endTime;
        }

    }

    /**
     * ??????????????????
     * ????????????????????????,???????????????id?????????????????????
     * ????????????????????????,????????????id?????????????????????
     * @param expId ??????id
     * @param userId ??????id
     * @return ExperimentStartVO
     */
    @Override
    public ExperimentStartVO expStart(int expId, int userId) throws IOException {
        ExperimentRecord expRecord = selectExpRecord(expId, userId);
        if (expRecord == null) {
            throw new BadRequestException("?????????????????????");
        }
        if(expRecord.getIsClosed()!=0){
            DockerClient dockerClient = DockerClientUtil.connectDocker();
            dockerClientUtil.startContainer(dockerClient, expRecord.getContainerId());
            expRecord.setIsClosed(0);
            recordMapper.updateById(expRecord);
            dockerClient.close();
        }
        ExperimentStartVO expStartVO = new ExperimentStartVO();
        expStartVO.setContainerId(expRecord.getContainerId());
        expStartVO.setOccupyPort(expRecord.getOccupyPort());
        return expStartVO;
    }

    /**
     * ??????????????????
     * ??????????????????,????????????????????????
     * ??????????????????,????????????????????????
     * @param param ??????????????????
     * @return ??????????????????
     */
    @Override
    public boolean expClose(ExperimentCloseParam param) {
        Assert.notNull(param, "??????????????????????????????");

        int expId = param.getExperimentId();
        int userId = param.getUserId();
        int status;
        String containerId = param.getContainerId();
        ExperimentRecord expRecord = selectExpRecord(expId, userId);
        if (expRecord == null) {
            throw new BadRequestException("???????????????,????????????!");
        }
        if(expRecord.getIsClosed()==1){
            throw new BadRequestException("??????????????????");
        }
        if (containerId.equals(expRecord.getContainerId())) {
            dockerClientUtil.stopContainer(DockerClientUtil.connectDocker(), containerId);
            expRecord.setIsClosed(1);
            dockerClientUtil.removeContainer(DockerClientUtil.connectDocker(), containerId);
            expRecord.setIsRemoved(1);
            status = recordMapper.updateById(expRecord);
        } else {
            throw new BadRequestException("????????????id??????");
        }

        return status > 0;
    }


    /**
     * ????????????id?????????id??????????????????
     * @param experimentId ??????id
     * @param userId ??????id
     * @return ExperimentRecord
     */
    private ExperimentRecord selectExpRecord(int experimentId, int userId) {
        QueryWrapper<ExperimentRecord> queryWrapper = new QueryWrapper<ExperimentRecord>()
                .eq("experiment_id", experimentId)
                .eq("user_id", userId)
                .eq("is_removed", 0);
        return recordMapper.selectOne(queryWrapper);
    }

    /**
     * ????????????id,????????????,??????????????????????????????????????????????????????
     * @param experimentId ??????id
     * @return ExperimentEnterVO
     */
    private ExperimentEnterVO queryExperiment(int experimentId) {
        QueryWrapper<Experiment> queryWrapper = new QueryWrapper<Experiment>()
                .select("experiment_title", "course_detail", "experiment_task")
                .eq("experiment_id", experimentId);
        Experiment experiment = experimentMapper.selectOne(queryWrapper);
        ExperimentEnterVO experimentEnterVO = new ExperimentEnterVO();
        BeanUtils.copyProperties(experiment, experimentEnterVO);
        return experimentEnterVO;
    }

    private boolean queryClassExperiment(Map<String, Object> map) {
        return CollectionUtils.isEmpty(classExperimentMapper.selectByMap(map));
    }

}
