import QueueAnim from 'rc-queue-anim';
import TweenOne from 'rc-tween-one';
import PropTypes from 'prop-types';
import React, { Component } from "react";

import './list.css'
import { Form, Input, Select, Button, message } from "antd";

import { reqRegister } from "../../api";
import "./register.less";
import Identify from "../login/verifyCode";

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 8 }
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 16 }
  }
};
const tailFormItemLayout = {
  wrapperCol: {
    xs: {
      span: 24,
      offset: 0
    },
    sm: {
      span: 16,
      offset: 8
    }
  }
};

 class ListDemo extends React.Component {
  static propTypes = {
    className: PropTypes.string,
  };

  static defaultProps = {
    className: 'queue-demo',
  };

  constructor(props) {
    super(props);
    this.openIndex = null;
    this.position = {};
    this.state = {
      registerFlag: false,
      confirmDirty: false,
      autoCompleteResult: [],
      isChecked:false,
      
      animation: [],
      style: [],
    };
  }


  /**
   * @description: 获取表单数据,调用reqRegister接口传输。本质是ajax。传输数据，成功输出success，失败failed 
   * @param {e 点击事件} 
   * @return: 
   */
  handleSubmit = e => {
    e.preventDefault();
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err&&this.state.isChecked) {
        reqRegister(
          values.userPassword,
          values.user_tel,
          values.user_name,
          values.user_academy,
          values.user_class,
          values.user_studentnumber,
          values.user_email,
          values.user_gender,
          values.class_id
        )
          .then(response => {
            if (response.isSuccess) {
              message.success("注册成功"); 
              this.props.history.push("/login")
            } else {
              message.error(response.message);
            }
          })
          .catch(error => {
            message.success(error.message);
          });
      }
      else {
        message.error("注册信息有误！");
      }
    });
  };

  /**
   * @description: 比较两次密码
   * @param {rule, value 这次的密码值, callback 回调} 
   * @return: 
   */
  compareToFirstPassword = (rule, value, callback) => {
    const { form } = this.props;
    if (value && value !== form.getFieldValue("userPassword")) {
      callback("两次密码不一致");
    } else {
      callback();
    }
  };
  validateToNextPassword = (rule, value, callback) => {
    const { form } = this.props;
    if (value && this.state.confirmDirty) {
      form.validateFields(["user_password_repeat"], { force: true });
    }
    callback();
  };

  componentDidMount() {
    if (window.addEventListener) {
      window.addEventListener('touchend', this.onTouchEnd);
      window.addEventListener('mouseup', this.onTouchEnd);
    } else {
      window.attachEvent('ontouchend', this.onTouchEnd);
      window.attachEvent('onmouseup', this.onTouchEnd);
    }
  }

  componentWillUnmount() {
    if (window.addEventListener) {
      window.removeEventListener('touchend', this.onTouchEnd);
      window.removeEventListener('mouseup', this.onTouchEnd);
    } else {
      window.detachEvent('onresize', this.onTouchEnd);
      window.detachEvent('onmouseup', this.onTouchEnd);
    }
  }

  onTouchStart = (e, i) => {
    if (this.openIndex || this.openIndex === 0) {
      const animation = this.state.animation;
      animation[this.openIndex] = { x: 0, ease: 'easeOutBack' };
      this.setState({ animation }, () => {
        delete this.state.style[this.openIndex];
      });
      this.openIndex = null;
      return;
    }
    this.index = i;
    this.mouseXY = {
      startX: e.touches === undefined ? e.clientX : e.touches[0].clientX,
    };
  };

  onTouchEnd = () => {
    if (!this.mouseXY) {
      return;
    }
    const animation = this.state.animation;
    if (this.position[this.index] <= -60) {
      this.openIndex = this.index;
      animation[this.index] = { x: -60, ease: 'easeOutBack' };
    } else {
      animation[this.index] = { x: 0, ease: 'easeOutBack' };
    }

    delete this.mouseXY;
    delete this.position[this.index];
    this.index = null;
    this.setState({ animation });
  };

  onTouchMove = (e) => {
    if (!this.mouseXY) {
      return;
    }
    const currentX = e.touches === undefined ? e.clientX : e.touches[0].clientX;
    let x = currentX - this.mouseXY.startX;
    x = x > 10 ? 10 + (x - 10) * 0.2 : x;
    x = x < -60 ? -60 + (x + 60) * 0.2 : x;
    this.position[this.index] = x;
    const style = this.state.style;
    style[this.index] = { transform: `translateX(${x}px)` };
    const animation = [];
    this.setState({ style, animation });
  };

  render() {
    const { getFieldDecorator } = this.props.form;
         
    // const liChildren = this.state.dataArray.map((item) => {
    //   const { img, text, key } = item;
    //   return (<li
    //     key={key}
    //     onMouseMove={this.onTouchMove}
    //     onTouchMove={this.onTouchMove}
    //   >
    //     <TweenOne
    //       className={`${this.props.className}-content`}
    //       onTouchStart={e => this.onTouchStart(e, key)}
    //       onMouseDown={e => this.onTouchStart(e, key)}
    //       onTouchEnd={this.onTouchEnd}
    //       onMouseUp={this.onTouchEnd}
    //       animation={this.state.animation[key]}
    //       style={this.state.style[key]}
    //     >
    //       <div className={`${this.props.className}-img`}>
    //         <img src={img} width="44" height="44" onDragStart={e => e.preventDefault()} />
    //       </div>
    //       <p>{text}</p>
    //     </TweenOne>
    //   </li>);
    // });
    return (<div>
      <div className={`${this.props.className}-wrapper`}>
        <div className={this.props.className}>
          <div className={`${this.props.className}-header`}>
            <i />
            <span>Ant Motion</span>
          </div>
          <QueueAnim
            component="ul"
            animConfig={[
              { opacity: [1, 0], translateY: [0, 30] },
              { height: 0 },
            ]}
            ease={['easeOutQuart', 'easeInOutQuart']}
            duration={[550, 450]}
            interval={150}
          >
            <Form
          {...formItemLayout}
          name="register"
          onSubmit={this.handleSubmit}
          className="register-form"
        >
          <Form.Item label="用户学号" hasFeedback>
            {getFieldDecorator("user_studentnumber", {
              rules: [
                {
                  required: true,
                  message: "请输入您的学号"
                },
                {
                  pattern: new RegExp("^\\d{5,20}$", "g"),
                  message: "学号有误"
                }
              ]
            })(<Input placeholder={"请输入您的学号"} />)}
          </Form.Item>
          <Form.Item label="真实姓名" hasFeedback>
            {getFieldDecorator("user_name", {
              rules: [
                {
                  required: true,
                  message: "请输入您的真实姓名"
                },
                // {
                //   pattern: new RegExp("^[\u4e00-\u9fa5]+$", "g"),
                //   message: "真实姓名有误"
                // }
              ]
            })(<Input placeholder={"请输入您的真实姓名"} />)}
          </Form.Item>
          {/* 密码  userPassword */}
          <Form.Item label="用户密码" hasFeedback>
            {getFieldDecorator("userPassword", {
              rules: [
                {
                  required: true,
                  message: "请输入您的密码"
                },
                {
                  min: 6,
                  max: 15,
                  message: "密码为6-15位字符"
                },
                {
                  pattern: new RegExp("^\\w{6,15}$", "g"),
                  message: "密码必须为字母或者数字"
                },
                {
                  validator: this.validateToNextPassword
                }
              ]
            })(<Input.Password placeholder={"请输入您的密码"} />)}
          </Form.Item>

          {/* 确认密码  user_password_repeat */}
          <Form.Item label="确认密码" hasFeedback>
            {getFieldDecorator("user_password_repeat", {
              rules: [
                {
                  required: true,
                  message: "请再次输入您的密码"
                },
                {
                  validator: this.compareToFirstPassword,
                  message: "两次密码不一致"
                }
              ]
            })(
              <Input.Password
                onBlur={this.handleConfirmBlur}
                placeholder={"请再次输入您的密码"}
              />
            )}
          </Form.Item>

          <Form.Item label="班级序号" hasFeedback>
            {getFieldDecorator("class_id", {
              rules: [
                {
                  required: true,
                  message: "请输入您的序号"
                },
                {
                  type: "string",
                  message: "序号不正确"
                }
              ]
            })(<Input placeholder={"请输入您加入班级的序号"} />)}
          </Form.Item>

          <Form.Item label="性别">
            {getFieldDecorator("user_gender", {
              rules: [
                {
                  required: true,
                  message: "请输入您的性别"
                }
              ]
            })(
              <Select placeholder="请选择性别" onChange={this.changeGender}>
                <Select.Option value="1">男</Select.Option>
                <Select.Option value="0">女</Select.Option>

                {/* <Select.Option value="1">杭州国际服务工程学院</Select.Option>
                <Select.Option value="2">沈钧儒法学院</Select.Option> */}
              </Select>
            )}
          </Form.Item>

          {/* 手机号码  user_tel */}
          <Form.Item label="手机号" hasFeedback>
            {getFieldDecorator("user_tel", {
              rules: [
                {
                  required: true,
                  message: "请输入您的手机号"
                },
                {
                  pattern: new RegExp("^1[3456789]\\d{9}$", "g"),
                  message: "请输入正确的手机号"
                }
              ]
            })(<Input placeholder={"请输入您的手机号"} />)}
          </Form.Item>

          <Form.Item label="邮箱" hasFeedback>
            {getFieldDecorator("user_email", {
              rules: [
                {
                  type: "email",
                  message: "邮箱格式不正确"
                },
                {
                  required: true,
                  message: "请输入您的邮箱"
                }
              ]
            })(<Input placeholder={"请输入您的邮箱"} />)}
          </Form.Item>

          <Form.Item label="学院">
            {getFieldDecorator("user_academy", {
              rules: [
                {
                  required: true,
                  message: "请输入您的学院"
                }
              ]
            })(
              <Select placeholder="请选择学院">
                <Select.Option value="杭州国际服务工程学院">
                  {" "}
                  杭州国际服务工程学院{" "}
                </Select.Option>
                <Select.Option value="沈钧儒法学院">沈钧儒法学院</Select.Option>
                <Select.Option value="医学院">医学院</Select.Option>
                <Select.Option value="外国语学院">外国语学院</Select.Option>
                <Select.Option value="体育与健康学院">
                  {" "}
                  体育与健康学院{" "}
                </Select.Option>
                <Select.Option value="经济与管理学院">
                  {" "}
                  经济与管理学院{" "}
                </Select.Option>
                <Select.Option value="理学院">理学院</Select.Option>
                <Select.Option value="阿里巴巴商学院">
                  {" "}
                  阿里巴巴商学院{" "}
                </Select.Option>
                <Select.Option value="其他">其他</Select.Option>
                {/* <Select.Option value="1">杭州国际服务工程学院</Select.Option>
                <Select.Option value="2">沈钧儒法学院</Select.Option> */}
              </Select>
            )}
          </Form.Item>

          <Form.Item label="班级">
            {getFieldDecorator("user_class", {
             
            })(<Input placeholder="请输入您的班级，例如计科181班" />)}
          </Form.Item>
          <Form.Item {...tailFormItemLayout} lable="id_code">
            <Identify parent={ this }/>
          </Form.Item>
          <Form.Item {...tailFormItemLayout}>
            <Button
              type="primary"
              htmlType="submit"
              className="register-form-button"
            >
              注册
            </Button>
          </Form.Item>
        </Form>
          </QueueAnim>
        </div>
      </div>
    </div>);
  }
}

export default Form.create()(ListDemo);
