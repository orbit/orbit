import React, {useState} from 'react'
import './style.css'

import {Table} from 'antd'

const tableColumns = [
    {
        title: 'Type',
        dataIndex: 'type',
        key: 'type'
    },
    {
        title: 'Id',
        dataIndex: 'id',
        key: 'id'
    },
    {
        title: 'Node',
        dataIndex: 'nodeId'
    },
    {
        title: 'Messages',
        dataIndex: 'messageCount'
    }    
]

export default function Addressables(props) {
    const {addressables, select} = props;

    const [selected, setSelected] = useState(0)

    function selectRow(row) {
        if (selected !== row) {
            setSelected(row)
            select(row)
        }
    }

    const rows = addressables.map(a => ({
        ...a,
        key: a.id
    }))

    const rowSelection = {
        selectedRowKeys: [selected],
        type: 'radio',
        onChange: rows => selectRow(rows[0])
    };

    return (<section className="addressables">
            <h3>Addressables</h3>
            <Table className="addressables-table" dataSource={rows}
                   columns={tableColumns}
                   rowSelection={rowSelection}
                   pagination={false}
                   onRow={(record) => {
                       return {
                           onClick: () => selectRow(record.key)
                       }
                   }}
            />
        </section>
    )
}
