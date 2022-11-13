import React from "react";
import DashboardHistoryItem from "./Items/DashboardHistoryItem";

type DashboardHistoryProps = {
    items: any;
};

const DashboardHistory = ({ items }: DashboardHistoryProps) => {
    return (
        <div className="dashboard-history">
            <div>История донатов</div>
            {items.map((item: any, key: number) => (
                <DashboardHistoryItem item={item} key={key} />
            ))}
        </div>
    );
};

export default DashboardHistory;