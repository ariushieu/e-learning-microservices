/**
 * Thư viện dùng chung cho toàn hệ thống E-Learning. Module này không có class main và
 * không chạy độc lập, nó được 5 service nghiệp vụ import.
 *
 * <p><b>Chỉ chứa hợp đồng dùng chung giữa các service:</b>
 * <ul>
 *   <li>{@code dto} - vỏ response, phân trang, hình dạng lỗi</li>
 *   <li>{@code exception} - mã lỗi và handler dựng response lỗi thống nhất</li>
 *   <li>{@code event} - payload sự kiện Kafka, tên topic, loại sự kiện</li>
 * </ul>
 *
 * <p><b>Không được đưa vào đây:</b>
 * <ul>
 *   <li>JPA entity - entity là chi tiết bên trong của service sở hữu database. Dùng chung
 *       entity nghĩa là 5 service chung một mô hình dữ liệu, phá vỡ nguyên tắc
 *       database-per-service mà toàn bộ thiết kế database dựng lên để bảo vệ.</li>
 *   <li>DTO request/response riêng của một service - thứ chỉ một service dùng thì để
 *       trong service đó.</li>
 *   <li>Logic nghiệp vụ - sửa một dòng ở đây là phải build và triển khai lại cả 5 service.
 *       Module này càng phình to thì hệ thống càng giống một monolith bị chia nhỏ.</li>
 * </ul>
 *
 * @see com.hunre.sharedcommon.autoconfigure.SharedCommonAutoConfiguration
 */
package com.hunre.sharedcommon;
